// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

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
