// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

public struct EthSign: Codable {
    /// base-64 encoded data
    public let message: String

    /// ethereum address
    public let address: String
}
