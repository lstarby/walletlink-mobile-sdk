package com.coinbase.walletlink.models

import com.squareup.moshi.Json

internal enum class RequestEventType(val rawValue: String) {
    /**
     * Web3 related requests
     */
    @Json(name = "Web3Request") WEB3_REQUEST("Web3Request")
}
