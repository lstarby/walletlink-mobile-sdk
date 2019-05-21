package com.coinbase.walletlink.models

import com.coinbase.walletlink.interfaces.JsonSerializable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class JoinSessionMessage(
    @field:Json(name = "id") val requestId: Int,
    val sessionId: String,
    val sessionKey: String
) : JsonSerializable {
    override fun asJsonString(): String {
        val moshi = Moshi.Builder().build() // FIXME: hish - shared?
        val adapter = moshi.adapter<JoinSessionMessage>(JoinSessionMessage::class.java)
        return adapter.toJson(this)
    }
}