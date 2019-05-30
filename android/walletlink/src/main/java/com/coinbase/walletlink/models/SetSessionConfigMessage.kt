package com.coinbase.walletlink.models

import com.coinbase.walletlink.interfaces.JsonSerializable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class SetSessionConfigMessage(
    val type: ClientMessageType = ClientMessageType.SET_SESSION_CONFIG,
    @field:Json(name = "id") val requestId: Int,
    val sessionId: String,
    val webhookId: String,
    val webhookUrl: String,
    val metadata: Map<String, String>
) : JsonSerializable {
    override fun asJsonString(): String {
        val moshi = Moshi.Builder().build() // FIXME: hish - shared?
        val adapter = moshi.adapter<SetSessionConfigMessage>(SetSessionConfigMessage::class.java)
        return adapter.toJson(this)
    }
}
