// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBCore
import CBHTTP
import RxSwift

class WalletLinkAPI {
    /// Mark a given event as seen
    ///
    /// - Parameters:
    ///   - eventId: The event ID
    ///   - sessionId: The session ID
    ///   - secret: The session secret
    ///
    /// - Returns: A Single wrapping a ServerRequestDTO
    func markEventAsSeen(eventId: String, sessionId: String, secret: String, url: URL) -> Single<Void> {
        let credentials = Credentials(sessionId: sessionId, secret: secret)

        return HTTP.post(
            service: HTTPService(url: url),
            path: "/events/\(eventId)/seen",
            credentials: credentials
        )
        .asVoid()
        .logError()
        .catchErrorJustReturn(())
    }

    /// Fetch all unseen events
    ///
    /// - Parameters:
    ///   - session: WalletLink connection session
    ///
    /// - Returns: A Single wrapping a list of encrypted host requests
    func getUnseenEvents(session: Session) -> Single<[ServerRequestDTO]> {
        let credentials = Credentials(sessionId: session.id, secret: session.secret)

        return HTTP.get(
            service: HTTPService(url: session.url),
            path: "/events",
            credentials: credentials,
            parameters: ["unseen": "true"],
            for: GetEventsDTO.self
        )
        .map { response in
            response.body.events.map { event in
                ServerRequestDTO(
                    sessionId: session.id,
                    type: .event,
                    event: event.event,
                    eventId: event.id,
                    data: event.data
                )
            }
        }
    }
}
