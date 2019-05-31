package com.coinbase.walletlink.models.old

import com.coinbase.walletlink.interfaces.JsonSerializable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class PublishEventMessage(
    val type: ClientMessageType = ClientMessageType.PUBLISH_EVENT,
    @field:Json(name = "id") val requestId: Int,
    val sessionId: String,
    val event: String,
    val data: Map<String, String>
) : JsonSerializable {
    override fun asJsonString(): String {
        val moshi = Moshi.Builder().build() // FIXME: hish - shared?
        val adapter = moshi.adapter<PublishEventMessage>(PublishEventMessage::class.java)
        return adapter.toJson(this)
    }
}
