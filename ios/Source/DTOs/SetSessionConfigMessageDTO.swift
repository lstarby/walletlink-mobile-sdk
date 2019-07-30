// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import CBCore

struct SetSessionConfigMessageDTO: Codable, JSONDeserializable, JSONSerializable {
    /// Type of message
    let type: ClientMessageType = .setSessionConfig

    /// Client generated request ID
    let id: Int32

    /// Server generated session ID
    let sessionId: String

    /// Push notification webhook ID
    let webhookId: String

    /// Push notification webhook URL
    let webhookUrl: String

    let metadata: [String: String]
}
