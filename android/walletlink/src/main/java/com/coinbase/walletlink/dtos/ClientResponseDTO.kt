// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.interfaces.JsonSerializable
import com.coinbase.wallet.core.util.JSON
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
    @ExperimentalUnsignedTypes
    override fun asJsonString(): String = JSON.toJsonString(this)

    companion object {
        @ExperimentalUnsignedTypes
        fun fromJsonString(jsonString: String): ClientResponseDTO? = JSON.fromJsonString(jsonString)
    }
}
