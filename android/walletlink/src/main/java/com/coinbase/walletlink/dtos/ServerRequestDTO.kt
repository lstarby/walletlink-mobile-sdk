// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.interfaces.JsonSerializable
import com.coinbase.wallet.core.util.JSON
import com.coinbase.walletlink.models.EventType
import com.coinbase.walletlink.models.ServerMessageType

/**
 * @param sessionId Server generated session ID
 * @param type Server message type
 * @param event Server message event
 * @param eventId Server random generated eventId. Used to find a pending event from WalletLink server
 * @param data Encrypted event data
 */
internal data class ServerRequestDTO(
    val sessionId: String,
    val type: ServerMessageType,
    val event: EventType,
    val eventId: String,
    val data: String
) : JsonSerializable {
    @ExperimentalUnsignedTypes
    override fun asJsonString(): String = JSON.toJsonString(this)

    companion object {
        @ExperimentalUnsignedTypes
        fun fromJsonString(jsonString: String): ServerRequestDTO? = JSON.fromJsonString(jsonString)
    }
}
