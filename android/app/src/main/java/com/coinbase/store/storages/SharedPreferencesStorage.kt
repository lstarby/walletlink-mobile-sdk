package com.coinbase.store.storages

import android.content.Context
import com.coinbase.store.interfaces.Storage
import com.orhanobut.hawk.Hawk

class SharedPreferencesStorage(context: Context) : Storage {
    init {
        Hawk.init(context).build()
    }

    override fun <T> set(key: String, value: T?, clazz: Class<T>) {
        if (value == null) {
            Hawk.delete(key)
        }

        Hawk.put(key, value)
    }

    override fun <T> get(key: String, clazz: Class<T>): T? {
        return Hawk.get(key)
    }

    override fun destroy() {
        Hawk.deleteAll()
    }
}