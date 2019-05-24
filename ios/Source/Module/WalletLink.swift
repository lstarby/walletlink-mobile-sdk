// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import RxSwift

public class WalletLink: WalletLinkProtocol {
    private let webhookId: String
    private let webhookUrl: URL
    private let disposeBag = DisposeBag()
    private var connections = [String: WalletLinkConnection]()
    private var connectionAccessQueue = DispatchQueue(label: "WalletLink.connectionAccessQueue")
    private let signatureRequestSubject = PublishSubject<SignatureRequest>()
    private let sessionStore = SessionStore()
    private let signatureRequestScheduler = SerialDispatchQueueScheduler(
        internalSerialQueueName: "WalletLink.signatureRequest"
    )

    public let signatureRequestObservable: Observable<SignatureRequest>

    public required init(webhookId: String, webhookUrl: URL) {
        self.webhookId = webhookId
        self.webhookUrl = webhookUrl

        signatureRequestObservable = signatureRequestSubject.asObservable()
    }

    public func connect(metadata: [ClientMetadataKey: String]) {
        // FIXME: hish - make one connection per RPC url
        connectionAccessQueue.sync {
            self.connections = sessionStore.sessions.reduce(into: [:]) { dict, session in
                let conn = WalletLinkConnection(
                    url: session.rpcUrl,
                    webhookId: webhookId,
                    webhookUrl: webhookUrl,
                    sessionStore: sessionStore,
                    metadata: metadata
                )

                dict[session.id] = conn
                self.observeConnection(conn)
            }
        }
    }

    public func disconnect() {
        connectionAccessQueue.sync {
            self.connections.removeAll()
        }
    }

    public func link(
        sessionId: String,
        secret: String,
        rpcURL: URL,
        metadata: [ClientMetadataKey: String]
    ) -> Single<Void> {
        var isLinked = false

        connectionAccessQueue.sync {
            isLinked = self.connections[sessionId] != nil
        }

        if isLinked {
            return .error(WalletLinkError.sessionAlreadyLinked)
        }

        let connection = WalletLinkConnection(
            url: rpcURL,
            webhookId: webhookId,
            webhookUrl: webhookUrl,
            sessionStore: sessionStore,
            metadata: metadata
        )

        return connection.link(sessionId: sessionId, secret: secret)
            .map { _ in
                // this means the link was successfully established. Cache the active connection
                self.connectionAccessQueue.sync {
                    self.connections[sessionId] = connection
                    self.observeConnection(connection)
                }
            }
    }

    public func setMetadata(key: ClientMetadataKey, value: String) -> Single<Void> {
        var setMetadatasingles: [Single<Void>]!

        connectionAccessQueue.sync {
            setMetadatasingles = self.connections.values
                .map { $0.setMetadata(key: key, value: value).catchErrorJustReturn(()) }
        }

        return Single.zip(setMetadatasingles).asVoid()
    }

    public func approve(sessionId: String, requestId: String, signedData: Data) -> Single<Void> {
        guard let conn = connection(for: sessionId) else { return .error(WalletLinkError.noConnectionFound) }

        return conn.approve(requestId: requestId, signedData: signedData)
    }

    public func reject(sessionId: String, requestId: String) -> Single<Void> {
        guard let conn = connection(for: sessionId) else { return .error(WalletLinkError.noConnectionFound) }

        return conn.reject(requestId: requestId)
    }

    // MARK: - Helpers

    private func connection(for sessionId: String) -> WalletLinkConnection? {
        var connection: WalletLinkConnection?

        connectionAccessQueue.sync {
            connection = connections[sessionId]
        }

        return connection
    }

    private func observeConnection(_ conn: WalletLinkConnection) {
        conn.signatureRequestObservable
            .map { request -> SignatureRequest? in request }
            .catchErrorJustReturn(nil)
            .unwrap()
            .observeOn(signatureRequestScheduler)
            .subscribe(onNext: { [weak self] request in
                self?.signatureRequestSubject.onNext(request)
            })
            .disposed(by: disposeBag)
    }
}
