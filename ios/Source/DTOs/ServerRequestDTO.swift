// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

import CBCore

struct ServerRequestDTO: Codable, JSONDeserializable, JSONSerializable {
    /// Server generated session ID
    let sessionId: String

    /// Server message type
    let type: ServerMessageType

    /// Server message event
    let event: EventType

    /// Server random generated eventId. Used to find a pending event from WalletLink server
    let eventId: String

    /// Encrypted event data
    let data: String
}
