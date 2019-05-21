package com.coinbase.store

import android.content.Context
import com.coinbase.store.interfaces.StoreInterface
import com.coinbase.store.models.StoreKey
import com.coinbase.store.models.StoreKind
import com.coinbase.store.storages.SharedPreferencesStorage
import com.coinbase.store.storages.MemoryStorage
import java.util.concurrent.locks.ReentrantReadWriteLock

// FIXME: hish - Figure out how to split encrypted vs non-encrypted

class Store(context: Context) : StoreInterface {
    private val appPrefStorage = SharedPreferencesStorage(context)
    private val memoryStorage = MemoryStorage()
    private val changeObservers = mutableMapOf<String, Any>()
    private val changeObserversLock = ReentrantReadWriteLock()

    override fun <T> set(key: StoreKey<T>, value: T?) {
        return when (key.kind) {
            StoreKind.SHARED_PREFERENCES -> appPrefStorage.set(key.name, value, key.clazz)
            StoreKind.MEMORY -> memoryStorage.set(key.name, value, key.clazz)
        }
    }

    override fun <T> get(key: StoreKey<T>): T? {
        return when (key.kind) {
            StoreKind.SHARED_PREFERENCES -> appPrefStorage.get(key.name, key.clazz)
            StoreKind.MEMORY -> memoryStorage.get(key.name, key.clazz)
        }
    }

    override fun <T> has(key: StoreKey<T>): Boolean {
        return get(key) != null
    }
}