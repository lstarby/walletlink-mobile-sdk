// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

struct GetEventDTO: Codable {
    let event: EventDTO?
    let error: String?
}

struct EventDTO: Codable {
    let id: String
    let event: RequestEventType
    let data: String
}
