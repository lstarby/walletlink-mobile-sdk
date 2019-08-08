package com.coinbase.walletlink.models

import com.squareup.moshi.Json

enum class ClientMetadataKey(val rawValue: String) {
    // Client ethereum address metadata key
    @Json(name = "EthereumAddress") EthereumAddress("EthereumAddress")
}
