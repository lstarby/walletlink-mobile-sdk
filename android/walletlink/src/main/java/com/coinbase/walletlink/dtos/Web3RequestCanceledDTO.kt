// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

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
