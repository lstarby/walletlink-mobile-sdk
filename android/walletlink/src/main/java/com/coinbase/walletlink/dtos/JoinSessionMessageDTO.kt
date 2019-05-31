package com.coinbase.walletlink.dtos

import com.coinbase.wallet.store.utils.JSON
import com.coinbase.walletlink.interfaces.JsonSerializable
import com.coinbase.walletlink.models.ClientMessageType

/**
 * Client message to join currently active WalletLink session
 *
 * @property type Type of message
 * @property id Client generated request ID
 * @property sesionId Server generated session ID
 * @property sessionKey Client computed session key
 */
internal data class JoinSessionMessageDTO(
    val type: ClientMessageType = ClientMessageType.JOIN_SESSION,
    val id: Int,
    val sessionId: String,
    val sessionKey: String
) : JsonSerializable {
    override fun asJsonString(): String = JSON.toJsonString(this)
}
