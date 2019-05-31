package com.coinbase.walletlink.dtos

import com.coinbase.wallet.store.utils.JSON
import com.coinbase.walletlink.interfaces.JsonSerializable
import com.coinbase.walletlink.models.ClientMessageType

/**
 * Client message to update metadata for given key
 *
 * @property type Type of message
 * @property id Client generated request ID
 * @property sessionId Server generated session ID
 * @property key Metadata key name
 */
internal data class SetMetadataMessageDTO(
    val type: ClientMessageType = ClientMessageType.SET_METADATA,
    val id: Int,
    val sessionId: String,
    val key: String,
    val value: String
) : JsonSerializable {
    override fun asJsonString(): String = JSON.toJsonString(this)
}
