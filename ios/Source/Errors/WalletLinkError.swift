// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

enum WalletLinkError: Error {
    /// Unable to encrypt data using shared secret
    case unableToEncryptData

    /// Unable to decrypt data using shared secret
    case unableToDecryptData

    /// Thrown if unable to find connection for given sessionId
    case noConnectionFound

    /// Thrown when WalletLink connection is unable to send data to server
    case unableToSendData

    /// Thrown when WalletLink is unable to to serialize message json
    case unableToSerializeMessageJSON

    /// Thrown when trying to conenct with an invalid session
    case invalidSession

    /// Thrown if unable to approve or reject signature request. This generally happens if no internet or internal
    /// server error
    case unableToSendSignatureRequestConfirmation
}
