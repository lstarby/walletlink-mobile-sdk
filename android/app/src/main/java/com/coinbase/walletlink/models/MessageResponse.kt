package com.coinbase.walletlink.models

data class MessageResponse(
    val type: ResponseType,
    val requestId: Int?,
    val sessionId: String
)  {
    enum class ResponseType {
        OK,
        FAIL
    }
}