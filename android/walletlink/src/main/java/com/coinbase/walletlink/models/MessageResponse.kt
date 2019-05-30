package com.coinbase.walletlink.models

import com.coinbase.walletlink.interfaces.JsonDeserializable
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi

data class MessageResponse(
    val type: ResponseType,
    val id: Int?,
    val sessionId: String
) {
    enum class ResponseType {
        @Json(name = "OK") OK,
        @Json(name = "Fail") FAIL
    }

    companion object : JsonDeserializable<MessageResponse> {
        override fun fromJsonString(jsonString: String): MessageResponse? {
            val moshi = Moshi.Builder().build() // FIXME: hish - shared?
            val adapter = moshi.adapter<MessageResponse>(MessageResponse::class.java)
            return adapter.fromJson(jsonString)
        }
    }
}