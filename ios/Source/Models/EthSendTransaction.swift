// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

public struct EthSendTransaction: Codable {
    /// Ethereum address
    public let from: String

    /// Ethereum address or null for contract publishing
    public let to: String?

    /// Suggested gas limit. Stringified base-10 big integer or null (e.g. "21000")
    public let gas: String?

    /// Suggsted gas price. Stringified base-10 big integer or null (e.g. "1000000000")
    public let gasPrice: String?

    /// Transaction wei value. stringified base-10 big integer (e.g. "1500000000000000000")
    public let value: String

    /// Hex encoded data
    public let data: String

    /// Suggested transaction Nonce
    public let nonce: Int?

    /// Transaction chainId
    public let chainId: Int
}
