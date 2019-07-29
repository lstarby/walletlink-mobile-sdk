// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import RxSwift

final class RequestRepository {
    private let sessionStore: SessionStore
    private let api = WalletLinkAPI()

    init(sessionStore: SessionStore) {
        self.sessionStore = sessionStore
    }

    /// Mark requests as seen to prevent future presentation
    ///
    /// - Parameters:
    ///     - requestId: WalletLink host generated request ID
    ///     - url: The URL for the session
    ///
    /// - Returns: A single wrapping `Void` if operation was successful. Otherwise, an exception is thrown
    func markAsSeen(requestId: HostRequestId, url: URL) -> Single<Void> {
        guard let session = sessionStore.getSession(id: requestId.sessionId, url: url) else {
            return .justVoid()
        }

        return api.markEventAsSeen(eventId: requestId.eventId, sessionId: session.id, secret: session.secret, url: url)
    }

    /// Get pending requests for given sessionID. Canceled requests will be filtered out
    ///
    /// - Parameters:
    ///     - sessionId: Session ID
    ///     - url: The URL of the session
    ///
    /// - Returns: List of pending requests
    func getPendingRequests(session: Session, url: URL) -> Single<[HostRequest]> {
        return api.getUnseenEvents(sessionId: session.id, secret: session.secret, url: url)
            .map { requests in requests.compactMap { $0.asHostRequest(secret: session.secret, url: url) } }
            .map { requests in
                // build list of cancelation requests
                let cancelationRequests = requests.filter { $0.hostRequestId.isCancelation }

                // build list of pending requests by filtering out canceled requests
                let pendingRequests = requests.filter { request in
                    guard
                        let cancelationRequest = cancelationRequests.first(where: {
                            $0.hostRequestId.canCancel(request.hostRequestId)
                        })
                    else { return true }

                    self.markCancelledEventAsSeen(
                        requestId: request.hostRequestId,
                        cancelationRequestId: cancelationRequest.hostRequestId,
                        url: url
                    )

                    return false
                }

                return pendingRequests
            }
            .catchErrorJustReturn([])
    }

    private func getPendingRequests(sessionId: String, secret: String, url: URL) -> Single<[HostRequest]> {
        return api.getUnseenEvents(sessionId: sessionId, secret: secret, url: url)
            .map { requests in requests.compactMap { $0.asHostRequest(secret: secret, url: url) } }
            .map { requests in
                // build list of cancelation requests
                let cancelationRequests = requests.filter { $0.hostRequestId.isCancelation }

                // build list of pending requests by filtering out canceled requests
                let pendingRequests = requests.filter { request in
                    guard
                        let cancelationRequest = cancelationRequests.first(where: {
                            $0.hostRequestId.canCancel(request.hostRequestId)
                        })
                    else { return true }

                    self.markCancelledEventAsSeen(
                        requestId: request.hostRequestId,
                        cancelationRequestId: cancelationRequest.hostRequestId,
                        url: url
                    )

                    return false
                }

                return pendingRequests
            }
            .catchErrorJustReturn([])
    }

    private func markCancelledEventAsSeen(requestId: HostRequestId, cancelationRequestId: HostRequestId, url: URL) {
        _ = markAsSeen(requestId: requestId, url: url)
            .flatMap { _ in self.markAsSeen(requestId: cancelationRequestId, url: url) }
            .subscribe()
    }
}
