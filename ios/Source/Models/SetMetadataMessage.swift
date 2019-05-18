// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

/// Client message to update metadata for given key
struct SetMetadataMessage: Codable, JSONDeserializable, JSONSerializable {
    /// Type of message
    let type: ClientMessageType = .setMetadata

    /// Client generated request ID
    let requestId: Int32

    /// Server generated session ID
    let sessionId: String

    /// Metadata key name
    let key: String

    /// Metadata value
    let value: String

    enum CodingKeys: String, CodingKey {
        case type
        case requestId = "id"
        case sessionId
        case key
        case value
    }
}
