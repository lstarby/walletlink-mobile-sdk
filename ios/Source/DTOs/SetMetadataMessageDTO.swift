// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

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
