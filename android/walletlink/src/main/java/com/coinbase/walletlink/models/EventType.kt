// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.coinbase.walletlink.models

import com.squareup.moshi.Json

internal enum class EventType(val rawValue: String) {
    /**
     * Web3 related requests
     */
    @Json(name = "Web3Request") Web3Request("Web3Request"),

    /**
     * Web3 related responses
     */
    @Json(name = "Web3Response") Web3Response("Web3Response"),

    /**
     * Web3 request has been canceled by the host
     */
    @Json(name = "Web3RequestCanceled") Web3RequestCanceled("Web3RequestCanceled");
}
