// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBCore

/// The response triggered by a client request
struct ClientResponseDTO: Codable, JSONDeserializable, JSONSerializable {
    /// Type of message
    let type: ServerMessageType

    /// Client generated request ID
    let id: Int32?

    /// Server generated event ID
    let eventId: String?

    /// Server generated session ID
    let sessionId: String
}
