// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.coinbase.walletlink.models

import com.squareup.moshi.Json

// / Host request method
internal enum class RequestMethod(val rawValue: String) {
    /**
     * Request ethereum address from the user
     */
    @Json(name = "requestEthereumAccounts") RequestEthereumAccounts("requestEthereumAccounts"),

    /**
     * Ask user to sign a message using ethereum private key
     */
    @Json(name = "signEthereumMessage") SignEthereumMessage("signEthereumMessage"),

    /**
     * Ask user to sign (and optionally submit) an ethereum transaction
     */
    @Json(name = "signEthereumTransaction") SignEthereumTransaction("signEthereumTransaction"),

    /**
     * Ask the user to submit a transaction
     */
    @Json(name = "submitEthereumTransaction") SubmitEthereumTransaction("submitEthereumTransaction"),

    /**
     * Request was canceled on host side
     */
    @Json(name = "requestCanceled") RequestCanceled("requestCanceled");

    companion object {
        private val mapping = values().map { it.rawValue to it }.toMap()

        fun fromRawValue(rawValue: String): RequestMethod? {
            return mapping[rawValue]
        }
    }
}
