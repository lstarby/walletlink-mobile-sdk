package com.coinbase.networking.connectivity

// / Represent a network connection type
enum class ConnectionKind {
    /**
     * Connected over wifi
     */
    WIFI,

    /**
     * Connected over cell signal
     */
    CELL,

    /**
     * Unknown connection type
     */
    UNKNOWN;
}
