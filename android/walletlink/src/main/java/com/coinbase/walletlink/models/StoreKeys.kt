package com.coinbase.walletlink.models

import com.coinbase.wallet.store.models.EncryptedSharedPrefsStoreKey
import com.coinbase.wallet.store.models.SharedPrefsStoreKey

class StoreKeys {
    companion object {
        fun secret(sessionId: String): EncryptedSharedPrefsStoreKey<String> {
            return EncryptedSharedPrefsStoreKey(
                id = "walletlink_secret",
                uuid = sessionId,
                clazz = String::class.java
            )
        }

        val sessions = SharedPrefsStoreKey(id = "walletlink_sessions",
            syncNow = true,
            clazz = Array<String>::class.java
        )
    }
}
