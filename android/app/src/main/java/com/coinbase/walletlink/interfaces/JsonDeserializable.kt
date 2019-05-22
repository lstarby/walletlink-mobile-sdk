package com.coinbase.walletlink.interfaces

interface JsonDeserializable<T> {
    fun fromJsonString(jsonString: String): T?
}
