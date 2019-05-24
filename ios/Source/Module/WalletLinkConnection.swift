// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBCrypto
import CBHTTP
import os.log
import RxSwift

class WalletLinkConnection {
    private let webhookId: String
    private let webhookUrl: URL
    private let url: URL
    private var disposeBag = DisposeBag()
    private let sessionStore: SessionStore
    private let socket: WalletLinkWebSocket
    private let operationQueue = OperationQueue()
    private let isConnectedObservable: Observable<Bool>
    private let signatureRequestsSubject = PublishSubject<SignatureRequest>()
    private var metadata: [ClientMetadataKey: String]

    /// Incoming signature requests
    let signatureRequestObservable: Observable<SignatureRequest>

    /// Constructor
    ///
    /// - Parameters:
    ///     -  url: WalletLink server URL
    ///     - webhookId: Webhook ID used to push notifications to mobile client
    ///     - webhookUrl: Webhook URL used to push notifications to mobile client
    ///     - sessionStore: WalletLink session data store
    ///     - metadata: client metadata forwarded to host once link is established
    required init(
        url: URL,
        webhookId: String,
        webhookUrl: URL,
        sessionStore: SessionStore,
        metadata: [ClientMetadataKey: String]
    ) {
        self.url = url
        self.sessionStore = sessionStore
        self.webhookUrl = webhookUrl
        self.webhookId = webhookId
        self.metadata = metadata

        socket = WalletLinkWebSocket(url: url)
        operationQueue.maxConcurrentOperationCount = 1
        signatureRequestObservable = signatureRequestsSubject.asObservable()
        isConnectedObservable = socket.connectionStateObservable.map { $0.isConnected }

        sessionStore.observeSessions(for: url)
            .flatMap { [weak self] sessionIds -> Single<Void> in
                guard let self = self else { return .justVoid() }

                // If credentials list is not empty, try connecting to WalletLink server
                guard sessionIds.isEmpty else { return self.startConnection().catchErrorJustReturn(()) }

                // Otherwise, disconnect
                return self.stopConnection().catchErrorJustReturn(())
            }
            .subscribe()
            .disposed(by: disposeBag)

        socket.incomingHostEventsObservable
            .subscribe(onNext: { [weak self] event in
                self?.handleIncomingEvent(event)
            })
            .disposed(by: disposeBag)

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
    ///     - secret: WalletLink host/guest shared secret
    ///
    /// - Returns: A single wrapping `Void` if connection was successful. Otherwise, an exception is thrown
    func link(sessionId: String, secret: String) -> Single<Void> {
        let session = Session(id: sessionId, secret: secret, rpcUrl: url)

        // wait for connection to be established, then attempt to join and persist the new session.
        return isConnectedObservable
            .do(onSubscribe: {
                // Connect to WalletLink server (if disconnected)
                _ = self.startConnection().subscribe()
            })
            .filter { $0 == true }
            .takeSingle()
            .flatMap { _ in self.joinSession(session) }
            .map { success in
                if success {
                    return self.sessionStore.save(sessionId: session.id, secret: session.secret, rpcURL: self.url)
                }

                throw WalletLinkConnectionError.invalidSession
            }
            .timeout(15, scheduler: ConcurrentDispatchQueueScheduler(qos: .userInitiated))
            .logError()
    }

    /// Set metadata in all active sessions. This metadata will be forwarded to all the hosts
    ///
    /// - Parameters:
    ///   - key: Metadata key
    ///   - value: Metadata value
    ///
    /// - Returns: True if the operation succeeds
    func setMetadata(key: ClientMetadataKey, value: String) -> Single<Void> {
        metadata[key] = value

        let setMetadataSingles: [Single<Bool>] = sessionStore.getSessions(for: url).compactMap { session in
            guard
                let iv = Data.randomBytes(12),
                let encryptedValue = try? value.encryptUsingAES256GCM(secret: session.secret, iv: iv)
            else {
                assertionFailure("Unable to encrypt \(key):\(value)")
                return nil
            }

            return self.socket.setMetadata(key: key, value: encryptedValue, for: session.id)
                .logError()
                .catchErrorJustReturn(false) // FIXME: hish - should we handle error?
        }

        return Single.zip(setMetadataSingles).asVoid()
    }

    /// Send signature request approval to the requesting host
    ///
    /// - Parameters:
    ///     - requestId: WalletLink request ID
    ///     - signedData: User signed data
    ///
    /// - Returns: A single wrapping a `Void` if successful, or an exception is thrown
    func approve(requestId _: String, signedData _: Data) -> Single<Void> {
        return isConnectedObservable
            .filter { $0 }
            .takeSingle()
            .flatMap { _ in Single.just(()) }
    }

    /// Send signature request rejection to the requesting host
    ///
    /// - Parameters:
    ///     - requestId: WalletLink request ID
    ///
    /// - Returns: A single wrapping a `Void` if successful, or an exception is thrown
    func reject(requestId _: String) -> Single<Void> {
        return isConnectedObservable
            .filter { $0 }
            .takeSingle()
            .flatMap { _ in Single.just(()) }
    }

    // MARK: - Connection management

    private func startConnection() -> Single<Void> {
        operationQueue.cancelAllOperations()

        let connectSingle = Internet.statusChanges
            .filter { $0.isOnline }
            .takeSingle()
            .flatMap { _ in self.socket.connect() }
            .flatMap { self.joinSessions() }

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

    private func joinSessions() -> Single<Void> {
        let joinSessionSingles = sessionStore.getSessions(for: url)
            .map { self.joinSession($0).asVoid().catchErrorJustReturn(()) }

        return Single.zip(joinSessionSingles).asVoid()
    }

    private func joinSession(_ session: Session) -> Single<Bool> {
        let sessionKey = "\(session.id), \(session.secret) WalletLink".sha256()

        return socket.joinSession(using: sessionKey, for: session.id)
            .flatMap { success -> Single<Bool> in
                guard success else { return .just(false) }

                return self.setSessionConfig(session: session)
            }
            .map { success in
                if success {
                    os_log("[walletlink] successfully joined session %@", type: .debug, session.id)
                    return true
                } else {
                    os_log("[walletlink] Invalid session %@. Removing...", type: .error, session.id)
                    self.sessionStore.delete(sessionId: session.id)
                    return false
                }
            }
            .logError()
    }

    private func setSessionConfig(session: Session) -> Single<Bool> {
        guard let iv = Data.randomBytes(12) else { return .error(WalletLinkError.unableToEncryptData) }

        var encryptedMetadata = [String: String]()
        for (key, value) in metadata {
            guard let encryptedValue = try? value.encryptUsingAES256GCM(secret: session.secret, iv: iv) else {
                return .error(WalletLinkError.unableToEncryptData)
            }

            encryptedMetadata[key.rawValue] = encryptedValue
        }

        return socket.setSessionConfig(
            webhookId: webhookId,
            webhookUrl: webhookUrl,
            metadata: encryptedMetadata,
            for: session.id
        )
    }

    private func handleIncomingEvent(_ event: MessageEvent) {
        guard
            let session = sessionStore.getSession(id: event.sessionId, url: url),
            let decrypted = try? event.data.decryptUsingAES256GCM(secret: session.secret),
            let json = try? JSONSerialization.jsonObject(with: decrypted, options: [])
        else {
            return assertionFailure("Invalid event \(event)")
        }

        print("json \(json)")
    }
}
