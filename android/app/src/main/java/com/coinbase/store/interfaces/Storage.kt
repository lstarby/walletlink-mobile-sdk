package com.coinbase.store.interfaces

interface Storage {
    /**
     * Store value for key
     *
     * @param key Key used to store value
     * @param value Value to be stored. If nil is passed, the entry will be removed.
     * @param clazz Value class type
     */
    fun <T> set(key: String, value: T?, clazz: Class<T>)

    /**
     * Get value by key
     *
     * @param key Key to use to get value
     * @param clazz Value class type
     *
     * @return The stored value if available. Otherwise, nil
     */
    fun <T> get(key: String, clazz: Class<T>): T?

    // Delete all keys in storage
    fun destroy()
}