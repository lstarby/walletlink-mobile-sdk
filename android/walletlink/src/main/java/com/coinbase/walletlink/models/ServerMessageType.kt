package com.coinbase.walletlink.models

// Type of incoming server message
enum class ServerMessageType(val rawValue: String) {
    // New server initiated event
    EVENT("Event"),

    // A successful response to a client initiated request
    OK("OK"),

    // An error response to a client initiated request
    FAIL("Fail");

    companion object {
        private val mapping = values().map { it.rawValue to it }.toMap()

        fun fromRawValue(rawValue: String): ServerMessageType? {
            return mapping[rawValue]
        }
    }
}
