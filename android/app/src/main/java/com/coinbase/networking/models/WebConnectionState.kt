package com.coinbase.networking.models

// Represents connection state of any web connection
sealed class WebConnectionState

// The connection is currently live
class WebConnectionConnected : WebConnectionState()

// The connection is not currently live
data class WebConnectionDisconnected(val t: Throwable?) : WebConnectionState()

val WebConnectionState.isConnected: Boolean
    get() = when (this) {
        is WebConnectionConnected -> true
        is WebConnectionDisconnected -> false
    }