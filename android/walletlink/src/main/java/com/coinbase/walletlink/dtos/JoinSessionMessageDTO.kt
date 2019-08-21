// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.coinbase.walletlink.dtos

import com.coinbase.wallet.core.interfaces.JsonSerializable
import com.coinbase.wallet.core.util.JSON
import com.coinbase.walletlink.models.ClientMessageType

/**
 * Client message to join currently active WalletLink session
 *
 * @property type Type of message
 * @property id Client generated request ID
 * @property sessionId Server generated session ID
 * @property sessionKey Client computed session key
 */
internal data class JoinSessionMessageDTO(
    val type: ClientMessageType = ClientMessageType.JoinSession,
    val id: Int,
    val sessionId: String,
    val sessionKey: String
) : JsonSerializable {
    @ExperimentalUnsignedTypes
    override fun asJsonString(): String = JSON.toJsonString(this)
}
