// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

enum WalletLinkConnectionError: Error {
    /// Thrown when WalletLink connection is unable to send data to server
    case unableToSendData

    /// Thrown when WalletLink is unable to to serialize message json
    case unableToSerializeMessageJSON

    /// Thrown when trying to conenct with an invalid session
    case invalidSession
}
