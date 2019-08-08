package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.util.JSON
import com.coinbase.walletlink.models.RequestMethod
import com.squareup.moshi.Types

internal class Web3ResponseDTO<T> private constructor(
    val id: String,
    val response: Web3Response<T>
) {
    constructor(
        id: String,
        method: RequestMethod,
        result: T
    ) : this(
        id = id,
        response = Web3Response(method, result, null)
    )

    constructor(
        id: String,
        method: RequestMethod,
        errorMessage: String?
    ) : this(
        id = id,
        response = Web3Response(method, null, errorMessage)
    )
}

internal inline fun <reified T> Web3ResponseDTO<T>.asJsonString(): String {
    val type = Types.newParameterizedType(Web3ResponseDTO::class.java, T::class.java)
    val adapter = JSON.moshi.adapter<Web3ResponseDTO<T>>(type)

    return adapter.toJson(this)
}

internal data class Web3Response<T>(val method: RequestMethod, val result: T?, val errorMessage: String?)
