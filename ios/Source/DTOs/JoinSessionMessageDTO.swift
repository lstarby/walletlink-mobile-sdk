// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

import CBCore

/// Client message to join currently active WalletLink session
struct JoinSessionMessageDTO: Codable, JSONDeserializable, JSONSerializable {
    /// Type of message
    let type: ClientMessageType = .joinSession

    /// Client generated request ID
    let id: Int32

    /// Server generated session ID
    let sessionId: String

    /// Client computed session key
    let sessionKey: String
}
