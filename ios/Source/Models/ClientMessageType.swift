// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

import Foundation

enum ClientMessageType: String, Codable {
    /// Client wants to join the current websocket connected session
    case joinSession = "JoinSession"

    /// Client wants to publish an event in response to an incoming server initiated event
    case publishEvent = "PublishEvent"

    /// Client wants to set custom metadata shared with host
    case setMetadata = "SetMetadata"

    /// Client wants to set client-specific session settings
    case setSessionConfig = "SetSessionConfig"
}
