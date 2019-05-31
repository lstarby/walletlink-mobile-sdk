package com.coinbase.walletlink.models

import com.squareup.moshi.Json

internal enum class ClientMessageType {
    // Client wants to join the current websocket connected session
    @Json(name = "JoinSession") JOIN_SESSION,

    // Client wants to publish an event in response to an incoming server initiated event
    @Json(name = "PublishEvent") PUBLISH_EVENT,

    // Client wants to set custom metadata shared with host
    @Json(name = "SetMetadata") SET_METADATA,

    // Client wants to set client-specific session settings
    @Json(name = "SetSessionConfig") SET_SESSION_CONFIG,
}
