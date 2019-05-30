package com.coinbase.walletlink.concurrency

data class Result<T>(
    val response: T? = null,
    val throwable: Throwable? = null
)
