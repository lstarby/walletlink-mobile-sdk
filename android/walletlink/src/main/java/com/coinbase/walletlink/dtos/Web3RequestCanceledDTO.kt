package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.util.JSON
import java.net.URL

internal data class Web3RequestCanceledDTO(
    val type: String = "Web3RequestCanceled",
    val id: String,
    val origin: URL
) {
    companion object {
        fun fromJson(json: ByteArray): Web3RequestCanceledDTO? = JSON.fromJsonString(String(json, Charsets.UTF_8))
    }
}
