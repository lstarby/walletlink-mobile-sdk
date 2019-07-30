// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

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
