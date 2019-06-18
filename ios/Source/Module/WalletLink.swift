// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBHTTP
import RxSwift

public class WalletLink: WalletLinkProtocol {
    private let notificationUrl: URL
    private var disposeBag = DisposeBag()
    private var connections = ConcurrentCache<URL, WalletLinkConnection>()
    private let requestsSubject = PublishSubject<HostRequest>()
    private let sessionStore = SessionStore()
    private let requestsScheduler = SerialDispatchQueueScheduler(internalSerialQueueName: "WalletLink.request")
    private let processedRequestIds = BoundedSet<HostRequestId>(maxSize: 3000)

    public let requestsObservable: Observable<HostRequest>

    public var sessions: [Session] {
        return sessionStore.sessions
    }

    public required init(notificationUrl: URL) {
        self.notificationUrl = notificationUrl

        requestsObservable = requestsSubject.asObservable()
    }

    public func connect(userId: String, metadata: [ClientMetadataKey: String]) {
        let connections = ConcurrentCache<URL, WalletLinkConnection>()
        let sessionsByUrl: [URL: [Session]] = sessionStore.sessions
            .reduce(into: [:]) { $0[$1.url, default: []].append($1) }

        sessionsByUrl.forEach { url, sessions in
            let conn = WalletLinkConnection(
                url: url,
                userId: userId,
                notificationUrl: notificationUrl,
                sessionStore: sessionStore,
                metadata: metadata
            )

            self.observeConnection(conn)
            sessions.forEach { connections[$0.url] = conn }
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
        url: URL,
        userId: String,
        metadata: [ClientMetadataKey: String]
    ) -> Single<Void> {
        if let connection = connections[url] {
            return connection.link(sessionId: sessionId, name: name, secret: secret)
        }

        let connection = WalletLinkConnection(
            url: url,
            userId: userId,
            notificationUrl: notificationUrl,
            sessionStore: sessionStore,
            metadata: metadata
        )

        connections[url] = connection

        return connection.link(sessionId: sessionId, name: name, secret: secret)
            .map { _ in self.observeConnection(connection) }
            .catchError { err in
                self.connections[url] = nil
                throw err
            }
    }

    public func unlink(session: Session) {
        sessionStore.delete(url: session.url, sessionId: session.id)
    }

    public func setMetadata(key: ClientMetadataKey, value: String) -> Single<Void> {
        let setMetadataSingles = connections.values
            .map { $0.setMetadata(key: key, value: value).catchErrorJustReturn(()) }

        return Single.zip(setMetadataSingles).asVoid()
    }

    public func approve(requestId: HostRequestId, signedData: Data) -> Single<Void> {
        guard let connection = connections[requestId.url] else { return .error(WalletLinkError.noConnectionFound) }

        return connection.approve(
            sessionId: requestId.sessionId,
            requestId: requestId.id,
            signedData: signedData
        )
    }

    public func approveDappPermission(requestId: HostRequestId, ethAddress: String) -> Single<Void> {
        guard let connection = connections[requestId.url] else { return .error(WalletLinkError.noConnectionFound) }

        return connection.approveDappPermission(
            sessionId: requestId.sessionId,
            requestId: requestId.id,
            ethAddress: ethAddress
        )
    }

    public func reject(requestId: HostRequestId) -> Single<Void> {
        guard let connection = connections[requestId.url] else {
            return .error(WalletLinkError.noConnectionFound)
        }

        return connection.reject(sessionId: requestId.sessionId, requestId: requestId.id)
    }

    public func getRequest(eventId: String, sessionId: String, url: URL) -> Single<HostRequest> {
        guard let session = sessionStore.getSession(id: sessionId, url: url) else {
            return .error(WalletLinkError.sessionNotFound)
        }

        return WalletLinkAPI(url: url).getEvent(eventId: eventId, sessionId: sessionId, secret: session.secret)
            .map { request in
                guard let signatureRequest = request.asHostRequest(secret: session.secret, url: url) else {
                    throw WalletLinkError.unableToParseEvent
                }

                return signatureRequest
            }
    }

    // MARK: - Helpers

    private func observeConnection(_ conn: WalletLinkConnection) {
        conn.requestsObservable
            .observeOn(requestsScheduler)
            .map { request -> HostRequest? in request }
            .catchErrorJustReturn(nil)
            .unwrap()
            .subscribe(onNext: { [weak self] request in
                let hostRequestId = request.hostRequestId

                guard let self = self, !self.processedRequestIds.has(hostRequestId) else { return }

                self.processedRequestIds.add(hostRequestId)
                self.requestsSubject.onNext(request)

            })
            .disposed(by: disposeBag)
    }
}
