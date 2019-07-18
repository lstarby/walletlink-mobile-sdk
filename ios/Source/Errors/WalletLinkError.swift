// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

public enum WalletLinkError: Error {
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

    /// A session with the given ID was not found
    case sessionNotFound

    /// An event with the given ID was not found
    case eventNotFound

    /// The event data could not be parsed
    case unableToParseEvent

    /// Thrown when an invalid server URL is provided
    case invalidServerUrl

    /// Thrown when trying to respond with data that's missing or invalid
    case missingResponseData
}
