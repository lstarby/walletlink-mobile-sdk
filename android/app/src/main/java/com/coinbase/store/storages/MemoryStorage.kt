package com.coinbase.store.storages

import com.coinbase.store.interfaces.Storage
import java.util.concurrent.ConcurrentHashMap

class MemoryStorage : Storage {
    private val storage = ConcurrentHashMap<String, Any?>()

    override fun <T> set(key: String, value: T?, clazz: Class<T>) {
        if (value == null) {
            storage.remove(key)
        }

        storage[key] = value
    }

    override fun <T> get(key: String, clazz: Class<T>): T? {
        val value = storage[key]

        if (clazz.isInstance(value)) {
            return clazz.cast(value)
        }

        return null
    }

    override fun destroy() {
        storage.clear()
    }
}