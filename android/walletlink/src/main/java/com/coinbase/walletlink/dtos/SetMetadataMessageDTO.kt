// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.interfaces.JsonSerializable
import com.coinbase.wallet.core.util.JSON
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
    val type: ClientMessageType = ClientMessageType.SetMetadata,
    val id: Int,
    val sessionId: String,
    val key: String,
    val value: String
) : JsonSerializable {
    @ExperimentalUnsignedTypes
    override fun asJsonString(): String = JSON.toJsonString(this)
}
