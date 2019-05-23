package com.coinbase.store.storages

import android.content.Context
import android.content.SharedPreferences
import com.coinbase.store.interfaces.Storage
import com.coinbase.store.models.StoreKey
import com.google.gson.Gson

class SharedPreferencesStorage(context: Context) : Storage {
    private val preferences = context.getSharedPreferences("CBStore.plaintext", Context.MODE_PRIVATE)
    private val gson = Gson()

    override fun <T> set(key: StoreKey<T>, value: T?) {
        val editor: SharedPreferences.Editor = if (value == null) {
            preferences.edit().putString(key.name, null)
        } else {
            val jsonString = gson.toJson(value)
            preferences.edit().putString(key.name, jsonString)
        }

        if (key.syncNow) {
            editor.commit()
        } else {
            editor.apply()
        }
    }

    override fun <T> get(key: StoreKey<T>): T? {
        val jsonString = preferences.getString(key.name, null) ?: return null

        return gson.fromJson(jsonString, key.clazz)
    }
}