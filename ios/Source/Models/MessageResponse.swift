// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

/// The server response object trigger by a client request
struct MessageResponse: Codable, JSONDeserializable, JSONSerializable {
    /// Type of message
    let type: ResponseType

    /// Client generated request ID
    let requestId: Int32?

    /// Server generated session ID
    let sessionId: String

    enum CodingKeys: String, CodingKey {
        case type
        case requestId = "id"
        case sessionId
    }
}

extension MessageResponse {
    /// Response status
    enum ResponseType: String, Codable {
        /// Client request successfully completed
        case ok = "OK"

        /// client request failed
        case fail = "Fail"
    }
}
