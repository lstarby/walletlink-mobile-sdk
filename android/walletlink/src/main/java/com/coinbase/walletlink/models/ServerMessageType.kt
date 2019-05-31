package com.coinbase.walletlink.models

import com.squareup.moshi.Json

/**
 * Type of incoming server message
 */
internal enum class ServerMessageType(val rawValue: String) {
    /**
     * New server initiated event
     */
    @Json(name = "Event") EVENT("Event"),

    /**
     * A successful response to a client initiated request
     */
    @Json(name = "OK") OK("OK"),

    /**
     * A successful response to a client `PublishEvent`
     */
    @Json(name = "PublishEventOK") PUBLISH_EVENT_OK("PublishEventOK"),

    /**
     * An error response to a client initiated request
     */
    @Json(name = "Fail") FAIL("Fail");

    /**
     * Determine whether server message is a successful one
     */
    val isOk: Boolean get() = when (this) {
        OK -> true
        PUBLISH_EVENT_OK -> true
        else -> false
    }

    companion object {
        private val mapping = values().map { it.rawValue to it }.toMap()

        fun fromRawValue(rawValue: String): ServerMessageType? {
            return mapping[rawValue]
        }
    }
}
