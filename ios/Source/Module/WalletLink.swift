// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import RxSwift

public class WalletLink: WalletLinkProtocol {
    private let webhookId: String
    private let webhookUrl: URL
    private let disposeBag = DisposeBag()
    private var connections = ConcurrentCache<URL, WalletLinkConnection>()
    private let requestsSubject = PublishSubject<HostRequest>()
    private let sessionStore = SessionStore()
    private let requestsScheduler = SerialDispatchQueueScheduler(internalSerialQueueName: "WalletLink.request")

    public let requestsObservable: Observable<HostRequest>

    public required init(webhookId: String, webhookUrl: URL) {
        self.webhookId = webhookId
        self.webhookUrl = webhookUrl

        requestsObservable = requestsSubject.asObservable()
    }

    public func connect(metadata: [ClientMetadataKey: String]) {
        let connections = ConcurrentCache<URL, WalletLinkConnection>()
        let sessionsByUrl: [URL: [Session]] = sessionStore.sessions
            .reduce(into: [:]) { $0[$1.rpcUrl, default: []].append($1) }

        sessionsByUrl.forEach { rpcUrl, sessions in
            let conn = WalletLinkConnection(
                url: rpcUrl,
                webhookId: webhookId,
                webhookUrl: webhookUrl,
                sessionStore: sessionStore,
                metadata: metadata
            )

            self.observeConnection(conn)
            sessions.forEach { connections[$0.rpcUrl] = conn }
        }

        self.connections = connections
    }

    public func disconnect() {
        connections.removeAll()
    }

    public func link(
        sessionId: String,
        secret: String,
        rpcURL: URL,
        metadata: [ClientMetadataKey: String]
    ) -> Single<Void> {
        if let connection = connections[rpcURL] {
            return connection.link(sessionId: sessionId, secret: secret)
        }

        let connection = WalletLinkConnection(
            url: rpcURL,
            webhookId: webhookId,
            webhookUrl: webhookUrl,
            sessionStore: sessionStore,
            metadata: metadata
        )

        self.connections[rpcURL] = connection

        return connection.link(sessionId: sessionId, secret: secret)
            .map { _ in self.observeConnection(connection) }
            .catchError { err in
                self.connections[rpcURL] = nil
                throw err
            }
    }

    public func setMetadata(key: ClientMetadataKey, value: String) -> Single<Void> {
        let setMetadatasingles = connections.values
            .map { $0.setMetadata(key: key, value: value).catchErrorJustReturn(()) }

        return Single.zip(setMetadatasingles).asVoid()
    }

    public func approve(requestId: HostRequestId, signedData: Data) -> Single<Void> {
        guard let connection = connections[requestId.rpcURL] else {
            return .error(WalletLinkError.noConnectionFound)
        }

        return connection.approve(
            sessionId: requestId.sessionId,
            requestId: requestId.id,
            signedData: signedData
        )
    }

    public func reject(requestId: HostRequestId) -> Single<Void> {
        guard let connection = connections[requestId.rpcURL] else {
            return .error(WalletLinkError.noConnectionFound)
        }

        return connection.reject(sessionId: requestId.sessionId, requestId: requestId.id)
    }

    // MARK: - Helpers

    private func observeConnection(_ conn: WalletLinkConnection) {
        conn.requestsObservable
            .observeOn(requestsScheduler)
            .map { request -> HostRequest? in request }
            .catchErrorJustReturn(nil)
            .unwrap()
            .subscribe(onNext: { [weak self] request in
                self?.requestsSubject.onNext(request)
            })
            .disposed(by: disposeBag)
    }
}

