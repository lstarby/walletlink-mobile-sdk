// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

struct MessageEvent: Codable, JSONDeserializable, JSONSerializable {
    /// Server generated session ID
    let sessionId: String

    /// Server event
    let event: Kind

    /// Server random generated eventID. Used to find a pending event from WalletLink server
    let eventId: String

    /// Encrypted event data
    let data: [String: String]
}

extension MessageEvent {
    enum Kind: String, Codable, Hashable {
        /// Sign a message using ethereum key
        case signMessage = "EthSign"

        /// Sign and send ethereum transaction
        case signAndSubmitTx = "EthSendTransaction"
    }
}
