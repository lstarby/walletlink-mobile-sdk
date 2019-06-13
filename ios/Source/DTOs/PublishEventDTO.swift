// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

struct PublishEventDTO: Codable, JSONDeserializable, JSONSerializable {
    /// Type of message
    let type: ClientMessageType = .publishEvent

    /// Client generated request ID
    let id: Int32

    /// Server generated session ID
    let sessionId: String

    /// Event response type
    let event: EventType

    /// AES256 GCM encrypted data
    let data: String
}
