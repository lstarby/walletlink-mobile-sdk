// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

public enum SignatureRequest {
    /// A message signature request
    case message(requestId: String, address: String, message: String, isPrefixed: Bool)

    /// A transaction signature request
    case transaction(requestId: String, transaction: UnsignedTransaction)

    /// WalletLink request Id
    var requestId: String {
        switch self {
        case let .message(requestId, _, _, _):
            return requestId
        case let .transaction(requestId, _):
            return requestId
        }
    }
}

// FIXME: hish
public struct UnsignedTransaction {}
