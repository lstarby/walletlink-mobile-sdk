// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

struct GetEventsDTO: Codable {
    let events: [EventDTO]
    let timestamp: Int
    let error: String?
}

struct GetEventDTO: Codable {
    let event: EventDTO?
    let error: String?
}

struct EventDTO: Codable {
    let id: String
    let event: RequestEventType
    let data: String
}
