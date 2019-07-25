// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

enum EventType: String, Codable {
    /// Web3 related requests
    case web3Request = "Web3Request"

    /// Web3 related responses
    case web3Response = "Web3Response"

    /// Web3 request has been canceled by the host
    case web3RequestCanceled = "Web3RequestCanceled"
}
