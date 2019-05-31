package com.coinbase.walletlink.models

import com.coinbase.wallet.store.models.EncryptedSharedPrefsStoreKey

internal object StoreKeys {
    /**
     * Store key to keeping track of WalletLink sessions
     */
    val sessions = EncryptedSharedPrefsStoreKey(id = "walletlink_sessions", clazz = Array<Session>::class.java)
}
