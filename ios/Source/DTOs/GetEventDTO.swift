// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

struct GetEventsDTO: Codable {
    let events: [EventDTO]
    let timestamp: UInt64
    let error: String?
}

struct EventDTO: Codable {
    let id: String
    let event: EventType
    let data: String
}
