package com.coinbase.walletlink.dtos

import com.coinbase.wallet.store.utils.JSON
import com.coinbase.walletlink.interfaces.JsonSerializable
import com.coinbase.walletlink.models.RequestEventType
import com.coinbase.walletlink.models.ServerMessageType

/**
 *
 * @param sessionId Server generated session ID
 * @param type Server message type
 * @param event Server message event
 * @param eventId Server random generated eventId. Used to find a pending event from WalletLink server
 * @param data Encrypted event data
 */
internal data class ServerRequestDTO(
    val sessionId: String,
    val type: ServerMessageType,
    val event: RequestEventType,
    val eventId: String,
    val data: String
) : JsonSerializable {
    override fun asJsonString(): String = JSON.toJsonString(this)

    companion object {
        fun fromJsonString(jsonString: String): ServerRequestDTO? = JSON.fromJsonString(jsonString)
    }
}
