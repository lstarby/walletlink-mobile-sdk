// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

struct ServerRequestDTO: Codable, JSONDeserializable, JSONSerializable {
    /// Server generated session ID
    let sessionId: String

    /// Server message type
    let type: ServerMessageType

    /// Server message event
    let event: EventType

    /// Server random generated eventId. Used to find a pending event from WalletLink server
    let eventId: String // FIXME: hish - change to requestId

    /// Encrypted event data
    let data: String
}
