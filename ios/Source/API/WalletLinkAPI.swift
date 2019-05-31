// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBHTTP
import RxSwift

class WalletLinkAPI {
    private let rpcUrl: URL

    init(rpcUrl: URL) {
        self.rpcUrl = rpcUrl
    }

    /// Fetch an event with a given ID
    ///
    /// - Parameters:
    ///   - eventId: The event ID
    ///   - sessionId: The session ID
    ///
    /// - Returns: A Single wrapping a ServerRequestDTO
    func getEvent(eventId: String, sessionId: String) -> Single<ServerRequestDTO> {
        guard let host = rpcUrl.host, let url = URL(string: "http://\(host).com") else {
            return .error(WalletLinkError.invalidRPCURL)
        }

        return HTTP.get(service: HTTPService(url: url), path: "/events/\(eventId)", for: GetEventDTO.self)
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
}
