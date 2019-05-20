//
//  EthSign.swift
//  WalletLink
//
//  Created by Hish on 5/20/19.
//  Copyright © 2019 Coinbase Inc. All rights reserved.
//

import Foundation

public struct EthSign: Codable {
    /// base-64 encoded data
    public let message: String

    /// ethereum address
    public let address: String
}
