// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.interfaces.JsonSerializable
import com.coinbase.wallet.core.util.JSON
import com.coinbase.walletlink.models.ClientMessageType
import com.coinbase.walletlink.models.EventType

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
    val type: ClientMessageType = ClientMessageType.PublishEvent,
    val id: Int,
    val sessionId: String,
    val event: EventType,
    val data: String
) : JsonSerializable {
    @ExperimentalUnsignedTypes
    override fun asJsonString(): String = JSON.toJsonString(this)
}
