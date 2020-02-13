// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

import BigInt
import CBCore
import CBCrypto
import CBHTTP
import CBStore
import os.log
import RxSwift

private typealias JoinSessionEvent = (sessionId: String, joined: Bool)

class WalletLinkConnection {
    private let userId: String
    private let notificationUrl: URL
    private let url: URL
    private let socket: WalletLinkWebSocket
    private let isConnectedObservable: Observable<Bool>
    private let joinSessionEventsSubject = PublishSubject<JoinSessionEvent>()
    private let requestsSubject = PublishSubject<HostRequest>()
    private var disposeBag = DisposeBag()
    private var metadata: [ClientMetadataKey: String]
    private let linkRepository: LinkRepository

    /// Incoming host requests for action
    let requestsObservable: Observable<HostRequest>

    /// Constructor
    ///
    /// - Parameters:
    ///     - url: WalletLink server URL
    ///     - websocketUrl: WalletLink websocket endpoint
    ///     - userId: User ID to deliver push notifications to
    ///     - notificationUrl: Webhook URL used to push notifications to mobile client
    ///     - metadata: client metadata forwarded to host once link is established
    ///     - linkRepository: manages links and dapps
    required init(
        url: URL,
        userId: String,
        notificationUrl: URL,
        metadata: [ClientMetadataKey: String],
        linkRepository: LinkRepository
    ) {
        self.url = url
        self.notificationUrl = notificationUrl
        self.userId = userId
        self.metadata = metadata
        self.linkRepository = linkRepository

        socket = WalletLinkWebSocket(url: url.appendingPathComponent("rpc"))
        requestsObservable = requestsSubject.asObservable()
        isConnectedObservable = socket.connectionStateObservable.map { $0.isConnected }

        socket.incomingRequestsObservable
            .flatMap { [weak self] dto -> Single<HostRequest?> in
                guard let self = self else { return Single.just(nil) }
                return self.linkRepository.getHostRequest(using: dto, url: self.url)
            }
            .unwrap()
            .subscribe(onNext: { [weak self] in self?.requestsSubject.onNext($0) })
            .disposed(by: disposeBag)

        observeConnection()
    }

    /// Stop connection when WalletLink instance is deallocated
    deinit {
        disposeBag = DisposeBag()
        _ = stopConnection().subscribe()
    }

    /// Connect to WalletLink server using parameters extracted from QR code scan
    ///
    /// - Parameters:
    ///     - sessionId: WalletLink host generated session ID
    ///     - secret: WalletLink host/guest shared secret
    ///
    /// - Returns: A single wrapping `Void` if connection was successful. Otherwise, an exception is thrown
    func link(sessionId: String, secret: String) -> Single<Void> {
        if let session = linkRepository.getSession(id: sessionId, url: url), session.secret == secret {
            return .justVoid()
        }

        return isConnectedObservable
            .do(onSubscribe: {
                self.linkRepository.saveSession(url: self.url, sessionId: sessionId, secret: secret)
            })
            .filter { $0 }
            .takeSingle()
            .flatMap { _ in self.joinSessionEventsSubject.filter { $0.sessionId == sessionId }.takeSingle() }
            .map { guard $0.joined else { throw WalletLinkError.invalidSession } }
            .timeout(15, scheduler: ConcurrentDispatchQueueScheduler(qos: .userInitiated))
            .logError()
            .do(onError: { _ in self.linkRepository.delete(url: self.url, sessionId: sessionId) })
    }

    /// Set metadata in all active sessions. This metadata will be forwarded to all the hosts
    ///
    /// - Parameters:
    ///   - key: Metadata key
    ///   - value: Metadata value
    ///
    /// - Returns: A single wrapping `Void` if operation was successful. Otherwise, an exception is thrown
    func setMetadata(key: ClientMetadataKey, value: String) -> Single<Void> {
        metadata[key] = value

        return linkRepository.getSessions(for: url)
            .compactMap { session -> Single<Bool>? in
                if let encryptedValue = try? value.encryptUsingAES256GCM(secret: session.secret) {
                    return self.socket.setMetadata(key: key, value: encryptedValue, for: session.id).logError()
                }

                print("Unable to encrypt \(key):\(value)")
                return nil
            }
            .zip()
            .asVoid()
    }

    /// Send signature request approval to the requesting host
    ///
    /// - Parameters:
    ///     - requestId: WalletLink host generated request ID
    ///     - responseData: Response data
    ///
    /// - Returns: A single wrapping `Void` if operation was successful. Otherwise, an exception is thrown
    func approve(requestId: HostRequestId, responseData: Data) -> Single<Void> {
        guard let session = linkRepository.getSession(id: requestId.sessionId, url: url) else {
            return .error(WalletLinkError.sessionNotFound)
        }

        let markEventAsSeen = linkRepository.markAsSeen(requestId: requestId, url: url)

        switch requestId.method {
        case .requestEthereumAccounts:
            guard let address = String(data: responseData, encoding: .utf8) else {
                return Single.error(WalletLinkError.missingResponseData)
            }

            let response = Web3ResponseDTO<[String]>(
                id: requestId.id,
                method: requestId.method,
                result: [address.lowercased()]
            )

            return markEventAsSeen
                .flatMap { _ in self.submitWeb3Response(response, session: session) }
                .flatMap { _ in
                    let dapp = Dapp(url: requestId.dappURL, name: requestId.dappName, logoURL: requestId.dappImageURL)
                    return self.linkRepository.saveDapp(dapp)
                }

        case .signEthereumMessage, .signEthereumTransaction, .submitEthereumTransaction:
            let response = Web3ResponseDTO<String>(
                id: requestId.id,
                method: requestId.method,
                result: responseData.toPrefixedHexString()
            )

            return markEventAsSeen.flatMap { _ in self.submitWeb3Response(response, session: session) }
        case .requestCanceled:
            return Single.error(WalletLinkError.unsupportedRequestMethodApproval)
        }
    }

    /// Send signature request rejection to the requesting host
    ///
    /// - Parameters:
    ///     - requestId: WalletLink host generated request ID
    ///
    /// - Returns: A single wrapping `Void` if operation was successful. Otherwise, an exception is thrown
    func reject(requestId: HostRequestId) -> Single<Void> {
        guard let session = linkRepository.getSession(id: requestId.sessionId, url: url) else {
            return .error(WalletLinkError.sessionNotFound)
        }

        let response = Web3ResponseDTO<String>(
            id: requestId.id,
            method: requestId.method,
            errorMessage: "User rejected signature request"
        )

        return linkRepository.markAsSeen(requestId: requestId, url: url)
            .flatMap { _ in self.submitWeb3Response(response, session: session) }
    }

    // MARK: - Connection management

    private func startConnection() -> Single<Void> {
        return Internet.statusChanges
            .filter { $0.isOnline }
            .takeSingle()
            .flatMap { _ in self.socket.connect() }
            .logError()
    }

    private func stopConnection() -> Single<Void> {
        return socket.disconnect()
            .logError()
            .catchErrorJustReturn(())
    }

    // MARK: - Session management

    private func joinSessions(sessions: [Session]) -> Single<Void> {
        return sessions.map { self.joinSession($0).asVoid().catchErrorJustReturn(()) }
            .zip()
            .map { _ in self.fetchPendingRequests() }
    }

    private func joinSession(_ session: Session) -> Single<Bool> {
        let credentials = Credentials(sessionId: session.id, secret: session.secret)

        return socket.joinSession(using: credentials.password, for: session.id)
            .flatMap { success -> Single<Bool> in
                guard success else {
                    self.joinSessionEventsSubject.onNext(JoinSessionEvent(sessionId: session.id, joined: false))

                    return .just(false)
                }

                return self.setSessionConfig(session: session)
            }
            .map { success in
                if success {
                    print("[walletlink] successfully joined session \(session.id)")

                    self.joinSessionEventsSubject.onNext(JoinSessionEvent(sessionId: session.id, joined: true))

                    return true
                } else {
                    print("[walletlink] Invalid session \(session.id). Removing...")

                    self.linkRepository.delete(url: self.url, sessionId: session.id)
                    self.joinSessionEventsSubject.onNext(JoinSessionEvent(sessionId: session.id, joined: false))

                    return false
                }
            }
            .logError()
    }

    private func setSessionConfig(session: Session) -> Single<Bool> {
        var encryptedMetadata = [String: String]()
        for (key, value) in metadata {
            guard let encryptedValue = try? value.encryptUsingAES256GCM(secret: session.secret) else {
                return .error(WalletLinkError.unableToEncryptData)
            }

            encryptedMetadata[key.rawValue] = encryptedValue
        }

        return socket.setSessionConfig(
            webhookId: userId,
            webhookUrl: notificationUrl,
            metadata: encryptedMetadata,
            for: session.id
        )
    }

    private func fetchPendingRequests() {
        _ = linkRepository.getSessions(for: url)
            .map { session in self.linkRepository.getPendingRequests(session: session) }
            .zip()
            .map { requests in requests.flatMap { $0 } }
            .logError()
            .subscribe(onSuccess: { requests in requests.forEach { self.requestsSubject.onNext($0) } })
    }

    // MARK: Request Handlers

    private func submitWeb3Response<T: Codable>(_ response: Web3ResponseDTO<T>, session: Session) -> Single<Void> {
        guard
            let json = response.asJSONString,
            let encryptedString = try? json.encryptUsingAES256GCM(secret: session.secret)
        else { return .error(WalletLinkError.unableToEncryptData) }

        return isConnectedObservable
            .filter { $0 }
            .takeSingle()
            .flatMap { _ in self.socket.publishEvent(.web3Response, data: encryptedString, to: session.id) }
            .map { guard $0 else { throw WalletLinkError.unableToSendSignatureRequestConfirmation } }
    }

    // MARK: - Observer(s)

    private func observeConnection() {
        var joinedSessionIds = Set<String>()
        let sessionSerialScheduler = SerialDispatchQueueScheduler(internalSerialQueueName: "serialSession")
        let connSerialScheduler = SerialDispatchQueueScheduler(internalSerialQueueName: "serialConn")

        let sessionChangesObservable = linkRepository.observeSessions(for: url)
            .distinctUntilChanged()
            .observeOn(connSerialScheduler)
            .concatMap { [weak self] sessions -> Single<[Session]> in
                guard let self = self else { return .just(sessions) }

                // If credentials list is not empty, try connecting to WalletLink server
                if !sessions.isEmpty {
                    return self.startConnection().map { sessions }.catchErrorJustReturn(sessions)
                }

                // Otherwise, disconnect
                return self.stopConnection().map { sessions }.catchErrorJustReturn(sessions)
            }

        Observable.combineLatest(isConnectedObservable, sessionChangesObservable)
            .debounce(0.3, scheduler: sessionSerialScheduler)
            .observeOn(sessionSerialScheduler)
            .concatMap { [weak self] isConnected, sessions -> Observable<Void> in
                guard let self = self else { return .justVoid() }

                if !isConnected {
                    joinedSessionIds.removeAll()
                    return .justVoid()
                }

                let currentSessionIds = Set<String>(sessions.map { $0.id })

                // remove unlinked sessions
                joinedSessionIds = joinedSessionIds.filter { currentSessionIds.contains($0) }

                let newSessions = sessions.filter { !joinedSessionIds.contains($0.id) }
                newSessions.forEach { joinedSessionIds.insert($0.id) }

                return self.joinSessions(sessions: newSessions).asObservable()
            }
            .catchErrorJustReturn(())
            .subscribe()
            .disposed(by: disposeBag)
    }
}
