// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBHTTP
import RxSwift

public class WalletLink: WalletLinkProtocol {
    private let userId: String
    private let notificationUrl: URL
    private var disposeBag = DisposeBag()
    private var connections = ConcurrentCache<URL, WalletLinkConnection>()
    private let requestsSubject = PublishSubject<HostRequest>()
    private let sessionStore = SessionStore()
    private let requestsScheduler = SerialDispatchQueueScheduler(internalSerialQueueName: "WalletLink.request")

    public let requestsObservable: Observable<HostRequest>

    public var sessions: [Session] {
        return sessionStore.sessions
    }

    public required init(userId: String, notificationUrl: URL) {
        self.userId = userId
        self.notificationUrl = notificationUrl

        requestsObservable = requestsSubject.asObservable()
    }

    public func connect(metadata: [ClientMetadataKey: String]) {
        let connections = ConcurrentCache<URL, WalletLinkConnection>()
        let sessionsByUrl: [URL: [Session]] = sessionStore.sessions
            .reduce(into: [:]) { $0[$1.rpcUrl, default: []].append($1) }

        sessionsByUrl.forEach { rpcUrl, sessions in
            let conn = WalletLinkConnection(
                url: rpcUrl,
                userId: userId,
                notificationUrl: notificationUrl,
                sessionStore: sessionStore,
                metadata: metadata
            )

            self.observeConnection(conn)
            sessions.forEach { connections[$0.rpcUrl] = conn }
        }

        self.connections = connections
    }

    public func disconnect() {
        disposeBag = DisposeBag()
        connections.removeAll()
    }

    public func link(
        sessionId: String,
        name: String,
        secret: String,
        rpcUrl: URL,
        metadata: [ClientMetadataKey: String]
    ) -> Single<Void> {
        if let connection = connections[rpcUrl] {
            return connection.link(sessionId: sessionId, name: name, secret: secret)
        }

        let connection = WalletLinkConnection(
            url: rpcUrl,
            userId: userId,
            notificationUrl: notificationUrl,
            sessionStore: sessionStore,
            metadata: metadata
        )

        connections[rpcUrl] = connection

        return connection.link(sessionId: sessionId, name: name, secret: secret)
            .map { _ in self.observeConnection(connection) }
            .catchError { err in
                self.connections[rpcUrl] = nil
                throw err
            }
    }

    public func unlink(session: Session) {
        sessionStore.delete(rpcURL: session.rpcUrl, sessionId: session.id)
    }

    public func setMetadata(key: ClientMetadataKey, value: String) -> Single<Void> {
        let setMetadataSingles = connections.values
            .map { $0.setMetadata(key: key, value: value).catchErrorJustReturn(()) }

        return Single.zip(setMetadataSingles).asVoid()
    }

    public func approve(requestId: HostRequestId, signedData: Data) -> Single<Void> {
        guard let connection = connections[requestId.rpcUrl] else { return .error(WalletLinkError.noConnectionFound) }

        return connection.approve(
            sessionId: requestId.sessionId,
            requestId: requestId.id,
            signedData: signedData
        )
    }

    public func approveDappPermission(requestId: HostRequestId, ethAddress: String) -> Single<Void> {
        guard let connection = connections[requestId.rpcUrl] else { return .error(WalletLinkError.noConnectionFound) }

        return connection.approveDappPermission(
            sessionId: requestId.sessionId,
            requestId: requestId.id,
            ethAddress: ethAddress
        )
    }

    public func reject(requestId: HostRequestId) -> Single<Void> {
        guard let connection = connections[requestId.rpcUrl] else {
            return .error(WalletLinkError.noConnectionFound)
        }

        return connection.reject(sessionId: requestId.sessionId, requestId: requestId.id)
    }

    public func getRequest(eventId: String, sessionId: String, rpcUrl: URL) -> Single<HostRequest> {
        guard let connection = connections[rpcUrl] else {
            return .error(WalletLinkError.noConnectionFound)
        }

        return connection.getRequest(eventId: eventId, sessionId: sessionId)
    }

    // MARK: - Helpers

    private func observeConnection(_ conn: WalletLinkConnection) {
        conn.requestsObservable
            .observeOn(requestsScheduler)
            .map { request -> HostRequest? in request }
            .catchErrorJustReturn(nil)
            .unwrap()
            .subscribe(onNext: { [weak self] in self?.requestsSubject.onNext($0) })
            .disposed(by: disposeBag)
    }
}
