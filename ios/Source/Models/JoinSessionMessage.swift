// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

/// Client message to join currently active WalletLink session
struct JoinSessionMessage: Codable, JSONDeserializable, JSONSerializable {
    /// Type of message
    let type: ClientMessageType = .joinSession

    /// Client generated request ID
    let requestId: Int32

    /// Server generated session ID
    let sessionId: String

    /// Client computed session key
    let sessionKey: String

    enum CodingKeys: String, CodingKey {
        case type
        case requestId = "id"
        case sessionId
        case sessionKey
    }
}
