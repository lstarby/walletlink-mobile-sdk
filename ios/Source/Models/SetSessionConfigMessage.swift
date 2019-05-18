// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

struct SetSessionConfigMessage: Codable, JSONDeserializable, JSONSerializable {
    /// Type of message
    let type: ClientMessageType = .setSessionConfig

    /// Client generated request ID
    let requestId: Int32

    /// Server generated session ID
    let sessionId: String

    /// Client generated request ID
    let webhookId: String

    /// Server generated session ID
    let webhookUrl: String

    let metadata: [String: String]

    enum CodingKeys: String, CodingKey {
        case type
        case requestId = "id"
        case sessionId
        case webhookId
        case webhookUrl
        case metadata
    }
}
