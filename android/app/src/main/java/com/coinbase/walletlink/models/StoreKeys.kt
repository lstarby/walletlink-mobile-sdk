package com.coinbase.walletlink.models

import com.coinbase.store.models.SharedPrefsStoreKey

class StoreKeys {
    companion object {
        fun secret(sessionId: String): SharedPrefsStoreKey<String> {
            return SharedPrefsStoreKey("walletlink_secret", sessionId, String::class.java)
        }

        val sessions = SharedPrefsStoreKey("walletlink_sessions", clazz = Array<String>::class.java)
    }
}