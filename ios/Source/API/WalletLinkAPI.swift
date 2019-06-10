// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBHTTP
import RxSwift

class WalletLinkAPI {
    private let rpcUrl: URL

    private var restUrl: URL? {
        guard let host = rpcUrl.host, let url = URL(string: "http://\(host).com") else { return nil }

        return url
    }

    init(rpcUrl: URL) {
        self.rpcUrl = rpcUrl
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
        guard let url = restUrl else { return .error(WalletLinkError.invalidRPCURL) }

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
    ///   - date: The date to fetch events from
    ///   - sessionId: The session ID
    ///   - secret: The session secret
    ///
    /// - Returns: A Single wrapping a list of ServerRequestDTO
    func getEvents(since date: Date?, sessionId: String, secret: String) -> Single<(Date, [ServerRequestDTO])> {
        guard let url = restUrl else { return .error(WalletLinkError.invalidRPCURL) }

        let credentials = Credentials(username: sessionId, password: secret)

        var parameters: [String: String]?
        if let date = date {
            parameters = ["timestamp": "\(date.timeIntervalSince1970)"]
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

            return (Date(timeIntervalSince1970: TimeInterval(response.body.timestamp)), requests)
        }
    }
}
