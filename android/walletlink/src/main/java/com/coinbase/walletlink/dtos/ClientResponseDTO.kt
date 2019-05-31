package com.coinbase.walletlink.dtos

import com.coinbase.wallet.store.utils.JSON
import com.coinbase.walletlink.interfaces.JsonSerializable
import com.coinbase.walletlink.models.ServerMessageType

/**
 * The response triggered by a client request
 *
 *  @param type Type of message
 *  @param id Client generated request ID
 *  @param eventId Server generated event ID
 *  @param sessionId Server generated session ID
 */
internal data class ClientResponseDTO(
    val type: ServerMessageType,
    val id: Int?,
    val eventId: String?,
    val sessionId: String
) : JsonSerializable {
    override fun asJsonString(): String = JSON.toJsonString(this)

    companion object {
        fun fromJsonString(jsonString: String): ClientResponseDTO? = JSON.fromJsonString(jsonString)
    }
}
