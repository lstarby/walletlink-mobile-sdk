package com.coinbase.walletlink.interfaces

// Conformers to this interface can deserialize from a JSON string
interface JsonDeserializable<T> {
    fun fromJsonString(jsonString: String): T?
}
