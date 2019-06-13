// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBHTTP
import RxSwift

class WalletLinkAPI {
    private let url: URL

    init(url: URL) {
        self.url = url
    }

    /// Fetch an event with a given ID
    ///
    /// - Parameters:
    ///   - eventId: The event ID
    ///   - sessionId: The session ID
    ///   - secret: The session secret
    ///
    /// - Returns: A Single wrapping a ServerRequestDTO
    func getEvent(eventId: String, sessionId: String, secret: String) -> Single<ServerRequestDTO> {
        let credentials = Credentials(username: sessionId, password: secret)

        return HTTP.get(
            service: HTTPService(url: url),
            path: "/events/\(eventId)",
            credentials: credentials,
            for: GetEventDTO.self
        )
        .map { response in
            guard let event = response.body.event else { throw WalletLinkError.eventNotFound }

            return ServerRequestDTO(
                sessionId: sessionId,
                type: .event,
                event: event.event,
                eventId: event.id,
                data: event.data
            )
        }
    }

    /// Fetch all events since the given date
    ///
    /// - Parameters:
    ///   - timestamp: timestamp in seconds since last fetch
    ///   - sessionId: The session ID
    ///   - sessionKey: Generated session key
    ///
    /// - Returns: A Single wrapping a list of ServerRequestDTO
    func getEvents(
        since timestamp: UInt64?,
        sessionId: String,
        sessionKey: String
    ) -> Single<(UInt64, [ServerRequestDTO])> {
        let credentials = Credentials(username: sessionId, password: sessionKey)

        var parameters: [String: String]?
        if let timestamp = timestamp {
            parameters = ["timestamp": "\(timestamp)"]
        }

        return HTTP.get(
            service: HTTPService(url: url),
            path: "/events",
            credentials: credentials,
            parameters: parameters,
            for: GetEventsDTO.self
        )
        .map { response in
            let requests = response.body.events.map { event in
                ServerRequestDTO(
                    sessionId: sessionId,
                    type: .event,
                    event: event.event,
                    eventId: event.id,
                    data: event.data
                )
            }

            return (response.body.timestamp, requests)
        }
    }
}
