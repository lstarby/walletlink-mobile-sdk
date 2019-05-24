// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

public struct MessageEvent: Codable, JSONDeserializable, JSONSerializable {
    /// Server generated session ID
    public let sessionId: String

    /// Server event
    public let event: Kind

    /// Server random generated eventID. Used to find a pending event from WalletLink server
    public let eventId: String

    /// Encrypted event data
    public let data: String
}

extension MessageEvent {
    public enum Kind: String, Codable, Hashable {
        /// Web3 related requests
        case web3Request = "Web3Request"

        /// Sign a message using ethereum key
        case signMessage = "EthSign" // FIXME: hish - delete

        /// Sign and send ethereum transaction
        case signAndSubmitTx = "EthSendTransaction" // FIXME: hish - delete
    }
}
