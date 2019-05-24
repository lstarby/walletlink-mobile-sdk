// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

enum WalletLinkError: Error {
    /// Unable to encrypt data using shared secret
    case unableToEncryptData

    /// Throws when trying to link a session that's already connected
    case sessionAlreadyLinked

    /// Thrown if unable to find connection for given sessionId
    case noConnectionFound
}
