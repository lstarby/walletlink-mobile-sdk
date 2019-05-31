package com.coinbase.walletlink.models

import com.squareup.moshi.Json

// / Host request method
enum class RequestMethod(val rawValue: String) {
    /**
     * Request ethereum address from the user
     */
    @Json(name = "requestEthereumAddresses") REQUEST_ETHEREUM_ADDRESS("requestEthereumAddresses"),

    /**
     * Ask user to sign a message using ethereum private key
     */
    @Json(name = "signEthereumMessage") SIGN_ETHEREUM_MESSAGE("signEthereumMessage"),

    /**
     * Ask user to sign (and optionally submit) an ethereum transaction
     */
    @Json(name = "signEthereumTransaction") SIGN_ETHEREUM_TRANSACTION("signEthereumTransaction"),

    /**
     * Ask the user to submit a transaction
     */
    @Json(name = "submitEthereumTransaction") SUBMIT_ETHEREUM_TRANSATION("submitEthereumTransaction");

    companion object {
        private val mapping = values().map { it.rawValue to it }.toMap()

        fun fromRawValue(rawValue: String): RequestMethod? {
            return mapping[rawValue]
        }
    }
}
