// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

import Foundation

/// Host request method
enum RequestMethod: String, Codable {
    /// Request ethereum address from user
    case requestEthereumAccounts

    /// Ask user to sign a message using ethereum private key
    case signEthereumMessage

    /// Ask user to sign (and optionally submit) an ethereum transaction
    case signEthereumTransaction

    /// Ask the user to submit a transaction
    case submitEthereumTransaction

    /// Request was canceled on host side
    case requestCanceled
}
