// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

import Foundation

enum EventType: String, Codable {
    /// Web3 related requests
    case web3Request = "Web3Request"

    /// Web3 related responses
    case web3Response = "Web3Response"

    /// Web3 request has been canceled by the host
    case web3RequestCanceled = "Web3RequestCanceled"
}
