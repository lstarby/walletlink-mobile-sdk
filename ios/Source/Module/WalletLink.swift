// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBCrypto
import CBHTTP
import os.log
import RxSwift

public class WalletLink: WalletLinkProtocol {
    private var connectionDisposable: Disposable?
    private let linkStore = LinkStore()
    private let connection: WalletLinkConnection
    private let operationQueue = OperationQueue()
    private let isConnectedObservable: Observable<Bool>
    private let signatureRequestsSubject = PublishSubject<SignatureRequest>()
    private var metadata = [ClientMetadataKey: String]()

    /// Incoming signature requests
    public let signatureRequestObservable: Observable<SignatureRequest>

    /// Constructor
    ///
    /// - Parameters:
    ///     -  url: WalletLink server URL
    public required init(url: URL) {
        connection = WalletLinkConnection(url: url)
        operationQueue.maxConcurrentOperationCount = 1
        signatureRequestObservable = signatureRequestsSubject.asObservable()
        isConnectedObservable = connection.connectionStateObservable.map { $0.isConnected }
    }

    /// Stop connection when WalletLink instance is deallocated
    deinit {
        self.stop()
    }

    /// Starts WalletLink connection with the server if a stored session exists. Otherwise, this is a noop. This method
    /// should be called immediately on app launch.
    ///
    /// - Parameters:
    ///     - metadata: client metadata forwarded to host once link is established
    public func start(metadata: [ClientMetadataKey: String] = [:]) {
        self.metadata = metadata

        connectionDisposable?.dispose()

        connectionDisposable = linkStore.observeSessions()
            .flatMap { [weak self] sessionIds -> Single<Void> in
                guard let self = self else { return .justVoid() }

                // If credentials list is not empty, try connecting to WalletLink server
                guard sessionIds.isEmpty else { return self.startConnection().catchErrorJustReturn(()) }

                // Otherwise, disconnect
                return self.stopConnection().catchErrorJustReturn(())
            }
            .subscribe()
    }

    /// Disconnect from WalletLink server and stop observing session ID updates to prevent reconnection.
    public func stop() {
        operationQueue.cancelAllOperations()
        connectionDisposable?.dispose()
        connectionDisposable = nil
        _ = stopConnection().subscribe()
    }

    /// Connect to WalletLink server using parameters extracted from QR code scan
    ///
    /// - Parameters:
    ///     - sessionId: WalletLink host generated session ID
    ///     - secret: WalletLink host/guest shared secret
    ///
    /// - Returns: A single wrapping `Void` if connection was successful. Otherwise, an exception is thrown
    public func connect(sessionId: String, secret: String) -> Single<Void> {
        let session = Session(sessionId: sessionId, secret: secret)

        // Connect to WalletLink server (if disconnected)
        _ = startConnection().subscribe()

        // wait for connection to be established, then attempt to join and persist the new session.
        return isConnectedObservable
            .filter { $0 == true }
            .takeSingle()
            .flatMap { _ in self.joinSession(session) }
            .map { _ in self.linkStore.save(sessionId: session.sessionId, secret: session.secret) }
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
    public func setMetadata(key: ClientMetadataKey, value: String) -> Single<Void> {
        metadata[key] = value

        let setMetadataSingles: [Single<Bool>] = linkStore.sessions.compactMap { session in
            guard
                let iv = Data.randomBytes(12),
                let encryptedValue = try? value.encryptUsingAES256GCM(secret: session.secret, iv: iv)
            else {
                assertionFailure("Unable to encrypt \(key):\(value)")
                return nil
            }

            return self.connection.setMetadata(key: key, value: encryptedValue, for: session.sessionId)
                .logError()
                .catchErrorJustReturn(false)
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
    public func approve(requestId _: String, signedData _: Data) -> Single<Void> {
        return .just(())
    }

    /// Send signature request rejection to the requesting host
    ///
    /// - Parameters:
    ///     - requestId: WalletLink request ID
    ///
    /// - Returns: A single wrapping a `Void` if successful, or an exception is thrown
    public func reject(requestId _: String) -> Single<Void> {
        return .just(())
    }

    // MARK: - Connection management

    private func startConnection() -> Single<Void> {
        operationQueue.cancelAllOperations()

        let connectSingle = Internet.statusChanges
            .filter { $0.isOnline }
            .takeSingle()
            .flatMap { _ in self.connection.connect() }
            .flatMap { self.joinSessions() }

        return operationQueue.addSingle(connectSingle)
    }

    private func stopConnection() -> Single<Void> {
        operationQueue.cancelAllOperations()

        let disconnectSingle = connection.disconnect()
            .logError()
            .catchErrorJustReturn(())

        return operationQueue.addSingle(disconnectSingle)
    }

    // MARK: - Session management

    private func joinSessions() -> Single<Void> {
        let joinSessionSingles = linkStore.sessions.map { self.joinSession($0).asVoid().catchErrorJustReturn(()) }

        return Single.zip(joinSessionSingles).asVoid()
    }

    private func joinSession(_ session: Session) -> Single<Bool> {
        let sessionKey = "\(session.sessionId) \(session.secret) WalletLink".sha256()

        return connection.joinSession(using: sessionKey, for: session.sessionId)
            .flatMap { success -> Single<Bool> in
                guard success else { return .just(false) }

                return self.setSessionConfig(session: session)
            }
            .map { success in
                if success {
                    os_log("[walletlink] successfully joined session %@", type: .debug, session.sessionId)
                } else {
                    os_log("[walletlink] Invalid session %@. Removing...", type: .error, session.sessionId)
                    self.linkStore.delete(sessionId: session.sessionId)
                }

                return success
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

        return connection.setSessionConfig(
            webhookId: "1", // FIXME: hish - fill
            webhookUrl: "http://www.walletlink.org", // FIXME: hish - fill
            metadata: encryptedMetadata,
            for: session.sessionId
        )
    }
}
