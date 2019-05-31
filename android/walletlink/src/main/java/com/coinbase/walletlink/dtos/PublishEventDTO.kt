package com.coinbase.walletlink.dtos

import com.coinbase.wallet.store.utils.JSON
import com.coinbase.walletlink.interfaces.JsonSerializable
import com.coinbase.walletlink.models.ResponseEventType
import com.coinbase.walletlink.models.ClientMessageType

/**
 * Client message in response to a server initiated event
 *
 * @property type Type of message
 * @property id Client generated request ID
 * @property sessionId Server generated session ID
 * @property event Event response type
 * @property data AES256 GCM encrypted data
 */
internal data class PublishEventDTO(
    val type: ClientMessageType = ClientMessageType.PUBLISH_EVENT,
    val id: Int,
    val sessionId: String,
    val event: ResponseEventType,
    val data: String
) : JsonSerializable {
    override fun asJsonString(): String = JSON.toJsonString(this)
}
