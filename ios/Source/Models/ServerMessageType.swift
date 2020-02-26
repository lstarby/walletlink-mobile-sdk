// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

import Foundation

/// Type of incoming server message
enum ServerMessageType: String, Codable {
    /// New server initiated event
    case event = "Event"

    /// A successful response to a client initiated request
    case ok = "OK"

    /// A successful response to a client `PublishEvent`
    case publishEventOK = "PublishEventOK"

    /// Session configuration were updated
    case sessionConfigUpdated = "SessionConfigUpdated"

    /// A successful response for session configuration request
    case getSessionConfigOK = "GetSessionConfigOK"

    /// An error response to a client initiated request
    case fail = "Fail"

    var isOK: Bool {
        switch self {
        case .ok, .publishEventOK, .sessionConfigUpdated, .getSessionConfigOK:
            return true
        case .fail, .event:
            return false
        }
    }
}
