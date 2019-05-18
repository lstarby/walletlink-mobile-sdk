package com.coinbase.walletlink.models

import com.coinbase.walletlink.interfaces.JsonSerializable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

@JsonClass(generateAdapter = true)
data class SetMetadataMessage(
    @field:Json(name = "id") val requestId: Int,
    val sessionId: String,
    val key: String,
    val value: String
): JsonSerializable {
    override fun asJsonString(): String {
        val moshi = Moshi.Builder().build() // FIXME: hish - shared?
        val adapter = moshi.adapter<SetMetadataMessage>(SetMetadataMessage::class.java)
        return adapter.toJson(this)
    }
}