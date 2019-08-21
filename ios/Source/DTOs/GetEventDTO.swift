// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

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
