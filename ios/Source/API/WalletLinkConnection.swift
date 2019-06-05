// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import BigInt
import CBCrypto
import CBHTTP
import os.log
import RxSwift

private typealias JoinSessionEvent = (sessionId: String, joined: Bool)

class WalletLinkConnection {
    private let userId: String
    private let notificationUrl: URL
    private let url: URL
    private let sessionStore: SessionStore
    private let socket: WalletLinkWebSocket
    private let operationQueue = OperationQueue()
    private let isConnectedObservable: Observable<Bool>
    private let joinSessionEventsSubject = PublishSubject<JoinSessionEvent>()
    private let requestsSubject = PublishSubject<HostRequest>()
    private var disposeBag = DisposeBag()
    private var metadata: [ClientMetadataKey: String]
    private let api: WalletLinkAPI

    /// Incoming host requests for action
    let requestsObservable: Observable<HostRequest>

    /// Constructor
    ///
    /// - Parameters:
    ///     -  url: WalletLink server URL
    ///     - userId: User ID to deliver push notifications to
    ///     - notificationUrl: Webhook URL used to push notifications to mobile client
    ///     - sessionStore: WalletLink session data store
    ///     - metadata: client metadata forwarded to host once link is established
    required init(
        url: URL,
        userId: String,
        notificationUrl: URL,
        sessionStore: SessionStore,
        metadata: [ClientMetadataKey: String]
    ) {
        self.url = url
        self.sessionStore = sessionStore
        self.notificationUrl = notificationUrl
        self.userId = userId
        self.metadata = metadata
        api = WalletLinkAPI(rpcUrl: url)

        socket = WalletLinkWebSocket(url: url)
        operationQueue.maxConcurrentOperationCount = 1
        requestsObservable = requestsSubject.asObservable()
        isConnectedObservable = socket.connectionStateObservable.map { $0.isConnected }

        socket.incomingRequestsObservable
            .subscribe(onNext: { [weak self] in self?.handleIncomingRequest($0) })
            .disposed(by: disposeBag)

        observeConnection()
    }

    /// Stop connection when WalletLink instance is deallocated
    deinit {
        operationQueue.cancelAllOperations()
        disposeBag = DisposeBag()
        _ = stopConnection().subscribe()
    }

    /// Connect to WalletLink server using parameters extracted from QR code scan
    ///
    /// - Parameters:
    ///     - sessionId: WalletLink host generated session ID
    ///     - name: Host name
    ///     - secret: WalletLink host/guest shared secret
    ///
    /// - Returns: A single wrapping `Void` if connection was successful. Otherwise, an exception is thrown
    func link(sessionId: String, name: String, secret: String) -> Single<Void> {
        if let session = sessionStore.getSession(id: sessionId, url: url), session.secret == secret {
            return .justVoid()
        }

        return isConnectedObservable
            .do(onSubscribe: {
                self.sessionStore.save(rpcUrl: self.url, sessionId: sessionId, name: name, secret: secret)
            })
            .filter { $0 }
            .takeSingle()
            .flatMap { _ in self.joinSessionEventsSubject.filter { $0.sessionId == sessionId }.takeSingle() }
            .map { guard $0.joined else { throw WalletLinkError.invalidSession } }
            .timeout(15, scheduler: ConcurrentDispatchQueueScheduler(qos: .userInitiated))
            .logError()
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

        let singles = sessionStore.getSessions(for: url).compactMap { session -> Single<Bool>? in
            if let encryptedValue = try? value.encryptUsingAES256GCM(secret: session.secret) {
                return self.socket.setMetadata(key: key, value: encryptedValue, for: session.id).logError()
            }

            assertionFailure("Unable to encrypt \(key):\(value)")
            return nil
        }

        return Single.zip(singles).asVoid()
    }

    /// Approves Dapp permission request EIP-1102
    ///
    /// - Parameters:
    ///     - sessionId: WalletLink host generated session ID
    ///     - requestId: WalletLink request ID
    ///     - ethereumAddress: Current Ethereum Address
    ///
    /// - Returns: A single wrapping `Void` if operation was successful. Otherwise, an exception is thrown
    func approveDappPermission(sessionId: String, requestId: String, ethAddress: String) -> Single<Void> {
        guard let session = sessionStore.getSession(id: sessionId, url: url) else {
            return .error(WalletLinkError.noConnectionFound)
        }

        let response = Web3ResponseDTO<[String]>(id: requestId, result: [ethAddress.lowercased()])

        return submitWeb3Response(response, session: session)
    }

    /// Send signature request approval to the requesting host
    ///
    /// - Parameters:
    ///     - sessionId: WalletLink host generated session ID
    ///     - requestId: WalletLink request ID
    ///     - signedData: User signed data
    ///
    /// - Returns: A single wrapping `Void` if operation was successful. Otherwise, an exception is thrown
    func approve(sessionId: String, requestId: String, signedData: Data) -> Single<Void> {
        guard let session = sessionStore.getSession(id: sessionId, url: url) else {
            return .error(WalletLinkError.noConnectionFound)
        }

        let response = Web3ResponseDTO<String>(id: requestId, result: signedData.toPrefixedHexString())

        return submitWeb3Response(response, session: session)
    }

    /// Send signature request rejection to the requesting host
    ///
    /// - Parameters:
    ///     - sessionId: WalletLink host generated session ID
    ///     - requestId: WalletLink request ID
    ///
    /// - Returns: A single wrapping `Void` if operation was successful. Otherwise, an exception is thrown
    func reject(sessionId: String, requestId: String) -> Single<Void> {
        guard let session = sessionStore.getSession(id: sessionId, url: url) else {
            return .error(WalletLinkError.noConnectionFound)
        }

        let response = Web3ResponseDTO<String>(id: requestId, errorMessage: "User rejected signature request")

        return submitWeb3Response(response, session: session)
    }

    /// Get a Host initiated request
    ///
    /// - Parameters:
    ///   - eventId: The request's event ID
    ///   - sessionId: The request's session ID
    ///
    /// - Returns: A Single wrapping the HostRequest
    func getRequest(eventId: String, sessionId: String) -> Single<HostRequest> {
        return api.getEvent(eventId: eventId, sessionId: sessionId)
            .map { request in
                guard let signatureRequest = self.parseRequest(request) else {
                    throw WalletLinkError.unableToParseEvent
                }

                return signatureRequest
            }
    }

    // MARK: - Connection management

    private func startConnection() -> Single<Void> {
        operationQueue.cancelAllOperations()

        let connectSingle = Internet.statusChanges
            .filter { $0.isOnline }
            .takeSingle()
            .flatMap { _ in self.socket.connect() }

        return operationQueue.addSingle(connectSingle)
    }

    private func stopConnection() -> Single<Void> {
        operationQueue.cancelAllOperations()

        let disconnectSingle = socket.disconnect()
            .logError()
            .catchErrorJustReturn(())

        return operationQueue.addSingle(disconnectSingle)
    }

    // MARK: - Session management

    private func joinSessions(sessions: [Session]) -> Single<Void> {
        let joinSessionSingles = sessions.map { self.joinSession($0).asVoid().catchErrorJustReturn(()) }

        return Single.zip(joinSessionSingles).asVoid()
    }

    private func joinSession(_ session: Session) -> Single<Bool> {
        let sessionKey = "\(session.id), \(session.secret) WalletLink".sha256()

        return socket.joinSession(using: sessionKey, for: session.id)
            .flatMap { success -> Single<Bool> in
                guard success else {
                    self.joinSessionEventsSubject.onNext(JoinSessionEvent(sessionId: session.id, joined: false))

                    return .just(false)
                }

                return self.setSessionConfig(session: session)
            }
            .map { success in
                if success {
                    os_log("[walletlink] successfully joined session %@", type: .debug, session.id)

                    self.joinSessionEventsSubject.onNext(JoinSessionEvent(sessionId: session.id, joined: true))

                    return true
                } else {
                    os_log("[walletlink] Invalid session %@. Removing...", type: .error, session.id)

                    self.sessionStore.delete(rpcURL: self.url, sessionId: session.id)
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

    // MARK: Request Handlers

    private func handleIncomingRequest(_ request: ServerRequestDTO) {
        guard let request = parseRequest(request) else { return }
        requestsSubject.onNext(request)
    }

    private func parseRequest(_ request: ServerRequestDTO) -> HostRequest? {
        guard
            let session = sessionStore.getSession(id: request.sessionId, url: url),
            let decrypted = try? request.data.decryptUsingAES256GCM(secret: session.secret),
            let json = try? JSONSerialization.jsonObject(with: decrypted, options: []) as? [String: Any]
        else {
            assertionFailure("Invalid request \(request)")
            return nil
        }

        switch request.event {
        case .web3Request:
            guard
                let requestObject = json?["request"] as? [String: Any],
                let requestMethodString = requestObject["method"] as? String,
                let method = RequestMethod(rawValue: requestMethodString),
                let web3Request = parseWeb3Request(request, method: method, data: decrypted)
            else {
                assertionFailure("Invalid web3Request \(request)")
                return nil
            }

            return web3Request
        }
    }

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

    private func parseWeb3Request(_ request: ServerRequestDTO, method: RequestMethod, data: Data) -> HostRequest? {
        switch method {
        case .requestEthereumAddresses:
            guard let dto = Web3RequestDTO<RequestEthereumAddressesParams>.fromJSON(data) else {
                assertionFailure("Invalid requestEthereumAddresses \(request)")
                return nil
            }

            let requestId = hostRequestId(web3Request: dto, serverRequest: request)

            return .dappPermission(requestId: requestId)
        case .signEthereumMessage:
            guard let dto = Web3RequestDTO<SignEthereumMessageParams>.fromJSON(data) else {
                assertionFailure("Invalid signEthereumMessage \(request)")
                return nil
            }

            let params = dto.request.params
            let requestId = hostRequestId(web3Request: dto, serverRequest: request)

            return .signMessage(
                requestId: requestId,
                address: params.address,
                message: params.message,
                isPrefixed: params.addPrefix
            )
        case .signEthereumTransaction:
            guard
                let dto = Web3RequestDTO<SignEthereumTransactionParams>.fromJSON(data),
                let weiValue = BigInt(dto.request.params.weiValue)
            else {
                assertionFailure("Invalid signEthereumTransaction \(request)")
                return nil
            }

            let params = dto.request.params
            let requestId = hostRequestId(web3Request: dto, serverRequest: request)

            return .signAndSubmitTx(
                requestId: requestId,
                fromAddress: params.fromAddress,
                toAddress: params.toAddress,
                weiValue: weiValue,
                data: params.data.dataUsingHexEncoding() ?? Data(),
                nonce: params.nonce,
                gasPrice: params.gasPriceInWei.asBigInt,
                gasLimit: params.gasLimit.asBigInt,
                chainId: params.chainId,
                shouldSubmit: params.shouldSubmit
            )
        case .submitEthereumTransaction:
            guard
                let dto = Web3RequestDTO<SubmitEthereumTransactionParams>.fromJSON(data),
                let signedTx = dto.request.params.signedTransaction.dataUsingHexEncoding()
            else {
                assertionFailure("Invalid SubmitEthereumTransactionParams \(request)")
                return nil
            }

            let params = dto.request.params
            let requestId = hostRequestId(web3Request: dto, serverRequest: request)

            return .submitSignedTx(requestId: requestId, signedTx: signedTx, chainId: params.chainId)
        }
    }

    private func hostRequestId<T>(web3Request: Web3RequestDTO<T>, serverRequest: ServerRequestDTO) -> HostRequestId {
        return HostRequestId(
            id: web3Request.id,
            sessionId: serverRequest.sessionId,
            eventId: serverRequest.eventId,
            rpcUrl: url,
            dappUrl: web3Request.origin,
            dappName: nil
        )
    }

    // MARK: - Observer(s)

    private func observeConnection() {
        var joinedSessionIds = Set<String>()
        let serialScheduler = SerialDispatchQueueScheduler(internalSerialQueueName: "WalletLink.observeConnection")
        let sessionChangesObservable = sessionStore.observeSessions(for: url)
            .distinctUntilChanged()
            .flatMap { [weak self] sessions -> Single<[Session]> in
                guard let self = self else { return .just(sessions) }

                // If credentials list is not empty, try connecting to WalletLink server
                if !sessions.isEmpty {
                    return self.startConnection().map { sessions }.catchErrorJustReturn(sessions)
                }

                // Otherwise, disconnect
                return self.stopConnection().map { sessions }.catchErrorJustReturn(sessions)
            }

        Observable.combineLatest(isConnectedObservable, sessionChangesObservable)
            .debounce(0.3, scheduler: serialScheduler)
            .observeOn(serialScheduler)
            .flatMap { [weak self] isConnected, sessions -> Observable<Void> in
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
