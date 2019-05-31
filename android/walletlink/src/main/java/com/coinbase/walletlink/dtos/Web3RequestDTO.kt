package com.coinbase.walletlink.dtos

import com.coinbase.wallet.store.utils.JSON
import com.coinbase.walletlink.models.RequestMethod
import com.squareup.moshi.Types
import java.net.URL

internal data class Web3RequestDTO<T>(
    val id: String,
    val origin: URL,
    val request: Web3Request<T>
) {
    companion object
}

internal inline fun <reified T> Web3RequestDTO.Companion.fromJsonString(json: String): Web3RequestDTO<T>? {
    val type = Types.newParameterizedType(Web3RequestDTO::class.java, T::class.java)
    val adapter = JSON.moshi.adapter<Web3RequestDTO<T>>(type)

    return adapter.fromJson(json)
}

internal data class Web3Request<T>(val method: RequestMethod, val params: T)

internal data class RequestEthereumAddressesParams(val appName: String)

internal data class SignEthereumMessageParams(val message: String, val address: String, val addPrefix: Boolean)

internal data class SignEthereumTransactionParams(
    val fromAddress: String,
    val toAddress: String?,
    val weiValue: String,
    val data: String,
    val nonce: Int?,
    val gasPriceInWei: String?,
    val gasLimit: String?,
    val chainId: Int,
    val shouldSubmit: Boolean
)

internal data class SubmitEthereumTransactionParams(val signedTransaction: String, val chainId: Int)
