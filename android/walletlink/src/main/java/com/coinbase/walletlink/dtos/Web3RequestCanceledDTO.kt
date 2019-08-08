package com.coinbase.walletlink.dtos

import java.net.URL

internal data class Web3RequestCanceledDTO(
    val type: String = "Web3RequestCanceled",
    val id: String,
    val origin: URL
) {
    companion object {
        fun fromJson(json: ByteArray): Web3RequestCanceledDTO? {
            TODO()
            // TODO: hish
        }
    }
}
