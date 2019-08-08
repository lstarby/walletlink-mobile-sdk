package com.coinbase.walletlink.extensions

import com.coinbase.wallet.store.models.EncryptedSharedPrefsStoreKey
import com.coinbase.wallet.store.models.StoreKeys
import com.coinbase.walletlink.models.Session

/**
 * Store key to keeping track of WalletLink sessions
 */
internal val StoreKeys.sessions: EncryptedSharedPrefsStoreKey<Array<Session>> by lazy {
    EncryptedSharedPrefsStoreKey(id = "walletlink_session_list", clazz = Array<Session>::class.java)
}
