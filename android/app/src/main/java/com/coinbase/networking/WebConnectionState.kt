package com.coinbase.networking

// Represents connection state of any web connection
sealed class WebConnectionState

// Represents connection state of any web connection
class WebConnectionConnected : WebConnectionState()

// The connection is currently live
data class WebConnectionDisconnected(val t: Throwable?) : WebConnectionState()