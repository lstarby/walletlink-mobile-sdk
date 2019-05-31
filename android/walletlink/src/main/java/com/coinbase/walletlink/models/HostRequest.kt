package com.coinbase.walletlink.models

import java.math.BigInteger

/**
 * Represents a host initiated request
 */
sealed class HostRequest {
    /**
     * message signature request
     */
    data class SignMessage(
        val requestId: HostRequestId,
        val address: String,
        val message: String,
        val isPrefixed: Boolean
    ) : HostRequest()

    /**
     * A transaction signature request
     */
    data class SignAndSubmitTx(
        val requestId: HostRequestId,
        val fromAddress: String,
        val toAddress: String?,
        val weiValue: BigInteger,
        val data: ByteArray,
        val nonce: Int?,
        val gasPrice: BigInteger?,
        val gasLimit: BigInteger?,
        val chainId: Int,
        val shouldSubmit: Boolean
    ) : HostRequest()

    /**
     * A signed transaction submission request
     */
    data class SubmitSignedTx(val requestId: HostRequestId, val signedTx: ByteArray, val chainId: Int) : HostRequest()

    /**
     * EIP 1102. Permission to allow message/transaction signature requests
     */
    data class DappPermission(val requestId: HostRequestId) : HostRequest()
}
