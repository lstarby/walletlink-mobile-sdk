// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

/// Client message to respond to a server-initiated event
struct PublishEventMessage: Codable, JSONDeserializable, JSONSerializable {
    /// Type of message
    let type: ClientMessageType = .publishEvent

    /// Client generated request ID
    let requestId: Int32

    /// Server generated session ID
    let sessionId: String

    /// Event initiated by server
    let event: String

    /// encrypted data
    let data: [String: String]

    enum CodingKeys: String, CodingKey {
        case type
        case requestId = "id"
        case sessionId
        case event
        case data
    }
}
