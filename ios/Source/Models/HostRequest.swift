// Copyright (c) 2017-2019 Coinbase Inc. See LICENSE

import BigInt

public enum HostRequest {
    /// A message signature request
    case signMessage(requestId: HostRequestId, address: String, message: String, isPrefixed: Bool)

    /// A transaction signature request
    case signAndSubmitTx(
        requestId: HostRequestId,
        fromAddress: String,
        toAddress: String?,
        weiValue: BigInt,
        data: Data,
        nonce: Int?,
        gasPrice: BigInt?,
        gasLimit: BigInt?,
        chainId: Int,
        shouldSubmit: Bool
    )

    /// A signed transaction submission request
    case submitSignedTx(requestId: HostRequestId, signedTx: Data, chainId: Int)

    /// EIP 1102. Permission to allow message/transaction signature requests
    case dappPermission(requestId: HostRequestId)

    /// The name of the dapp making the request
    public var dappName: String? {
        switch self {
        case let .signMessage(hostRequestId, _, _, _),
             let .signAndSubmitTx(hostRequestId, _, _, _, _, _, _, _, _, _),
             let .dappPermission(hostRequestId),
             let .submitSignedTx(hostRequestId, _, _):
            return hostRequestId.dappName
        }
    }

    /// WalletLink event ID
    var eventId: String {
        switch self {
        case let .signMessage(hostRequestId, _, _, _),
             let .signAndSubmitTx(hostRequestId, _, _, _, _, _, _, _, _, _),
             let .dappPermission(hostRequestId),
             let .submitSignedTx(hostRequestId, _, _):
            return hostRequestId.eventId
        }
    }

    /// WalletLink request ID
    var requestId: String {
        switch self {
        case let .signMessage(hostRequestId, _, _, _),
             let .signAndSubmitTx(hostRequestId, _, _, _, _, _, _, _, _, _),
             let .dappPermission(hostRequestId),
             let .submitSignedTx(hostRequestId, _, _):
            return hostRequestId.id
        }
    }

    /// WalletLink session ID
    var sessionId: String {
        switch self {
        case let .signMessage(hostRequestId, _, _, _),
             let .signAndSubmitTx(hostRequestId, _, _, _, _, _, _, _, _, _),
             let .dappPermission(hostRequestId),
             let .submitSignedTx(hostRequestId, _, _):
            return hostRequestId.sessionId
        }
    }
}
