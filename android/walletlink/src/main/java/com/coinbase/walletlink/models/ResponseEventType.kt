package com.coinbase.walletlink.models

import com.squareup.moshi.Json

internal enum class ResponseEventType(val rawValue: String) {
    /**
     * Web3 related response
     */
    @Json(name = "Web3Response") WEB3_RESPONSE("Web3Response")
}
