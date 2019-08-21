// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

import CBCore

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
