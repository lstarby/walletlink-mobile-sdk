package com.coinbase.walletlink.models.old

import com.coinbase.wallet.store.models.EncryptedSharedPrefsStoreKey
import com.coinbase.walletlink.models.Session

object StoreKeys {
    /**
     * Store key to keeping track of WalletLink sessions
     */
    val sessions = EncryptedSharedPrefsStoreKey(id = "walletlink_sessions",  clazz = Array<Session>::class.java)
}
