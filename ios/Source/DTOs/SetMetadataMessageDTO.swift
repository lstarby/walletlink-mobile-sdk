// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBCore

/// Client message to update metadata for given key
struct SetMetadataMessageDTO: Codable, JSONDeserializable, JSONSerializable {
    /// Type of message
    let type: ClientMessageType = .setMetadata

    /// Client generated request ID
    let id: Int32

    /// Server generated session ID
    let sessionId: String

    /// Metadata key name
    let key: String

    /// Metadata value
    let value: String
}
