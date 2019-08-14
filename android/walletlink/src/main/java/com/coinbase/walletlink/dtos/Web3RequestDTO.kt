package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.util.JSON
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

internal inline fun <reified T> Web3RequestDTO.Companion.fromJson(json: ByteArray): Web3RequestDTO<T>? {
    val jsonString = String(json, Charsets.UTF_8)
    return fromJsonString(jsonString)
}

internal data class Web3Request<T>(val method: RequestMethod, val params: T)

internal data class RequestEthereumAccountsParams(val appName: String, val appLogoUrl: URL?)

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
    val shouldSubmit: Boolean,
    val typedDataJson: String?
)

internal data class SubmitEthereumTransactionParams(val signedTransaction: String, val chainId: Int)
