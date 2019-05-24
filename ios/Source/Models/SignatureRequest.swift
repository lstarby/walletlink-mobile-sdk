// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import Foundation

public enum SignatureRequest {
    /// A message signature request
    case message(sessionId: String, requestId: String, address: String, message: String, isPrefixed: Bool)

    /// A transaction signature request
    case transaction(sessionId: String, requestId: String, transaction: EthSendTransaction)

    /// EIP 1102. Permission to allow message/transaction signature requests
  //  case dappPermission(sessionId: String, requestId: String, eventId: String)

    /// WalletLink request ID
    var requestId: String {
        switch self {
        case let .message(_, requestId, _, _, _):
            return requestId
        case let .transaction(_, requestId, _):
            return requestId
//        case let .dappPermission(_, requestId, _):
//            return requestId
        }
    }

    /// WalletLink session ID
    var sessionId: String {
        switch self {
        case let .message(sessionId, _, _, _, _):
            return sessionId
        case let .transaction(sessionId, _, _):
            return sessionId
//        case let .dappPermission(sessionId, _, _):
//            return sessionId
        }
    }
}
