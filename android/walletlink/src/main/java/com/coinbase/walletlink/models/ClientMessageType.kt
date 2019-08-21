// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.coinbase.walletlink.models

import com.squareup.moshi.Json

internal enum class ClientMessageType {
    /**
     * Client wants to join the current websocket connected session
     */
    @Json(name = "JoinSession") JoinSession,

    /**
     * Client wants to publish an event in response to an incoming server initiated event
     */
    @Json(name = "PublishEvent") PublishEvent,

    /**
     * Client wants to set custom metadata shared with host
     */
    @Json(name = "SetMetadata") SetMetadata,

    /**
     * Client wants to set client-specific session settings
     */
    @Json(name = "SetSessionConfig") SetSessionConfig,
}
