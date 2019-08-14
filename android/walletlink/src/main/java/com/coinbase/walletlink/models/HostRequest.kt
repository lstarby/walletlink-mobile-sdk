package com.coinbase.walletlink.models

import java.math.BigInteger
import java.net.URL

/**
 * Represents a host initiated request
 */
sealed class HostRequest(open val hostRequestId: HostRequestId) {
    /**
     * message signature request
     */
    data class SignMessage(
        override val hostRequestId: HostRequestId,
        val address: String,
        val message: String,
        val isPrefixed: Boolean,
        val typedDataJson: String?
    ) : HostRequest(hostRequestId)

    /**
     * A transaction signature request
     */
    @Suppress("ArrayInDataClass")
    data class SignAndSubmitTx(
        override val hostRequestId: HostRequestId,
        val fromAddress: String,
        val toAddress: String?,
        val weiValue: BigInteger,
        val data: ByteArray,
        val nonce: Int?,
        val gasPrice: BigInteger?,
        val gasLimit: BigInteger?,
        val chainId: Int,
        val shouldSubmit: Boolean
    ) : HostRequest(hostRequestId)

    /**
     * A signed transaction submission request
     */
    @Suppress("ArrayInDataClass")
    data class SubmitSignedTx(
        override val hostRequestId: HostRequestId,
        val signedTx: ByteArray,
        val chainId: Int
    ) : HostRequest(hostRequestId)

    /**
     * EIP 1102. Permission to allow message/transaction signature requests
     */
    data class DappPermission(override val hostRequestId: HostRequestId) : HostRequest(hostRequestId)

    /**
     * Request was canceled on host side
     */
    data class RequestCanceled(override val hostRequestId: HostRequestId) : HostRequest(hostRequestId)

    /**
     * The name of the dapp making the request
     */
    val dappName: String? get() = hostRequestId.dappName

    /**
     * The url of the dapp making the request
     */
    val dappUrl: URL get() = hostRequestId.dappURL

    /**
     * WalletLink event ID
     */
    val eventId: String get() = hostRequestId.eventId

    /**
     * WalletLink request ID
     */
    val requestId: String get() = hostRequestId.id

    /**
     * WalletLink session ID
     */
    val sessionId: String get() = hostRequestId.sessionId
}
