package com.coinbase.walletlink.dtos

import com.coinbase.wallet.store.utils.JSON
import com.squareup.moshi.Types

internal class Web3ResponseDTO<T>(
    val id: String,
    result: T? = null,
    errorMessage: String? = null
) {
    val response = Web3Response(result = result, errorMessage = errorMessage)
}

internal inline fun <reified T> Web3ResponseDTO<T>.asJsonString(): String {
    val type = Types.newParameterizedType(Web3ResponseDTO::class.java, T::class.java)
    val adapter = JSON.moshi.adapter<Web3ResponseDTO<T>>(type)

    return adapter.toJson(this)
}

internal data class Web3Response<T>(val result: T?, val errorMessage: String?)
