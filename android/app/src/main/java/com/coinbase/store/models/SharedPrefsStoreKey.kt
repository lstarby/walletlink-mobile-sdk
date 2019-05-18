package com.coinbase.store.models

class SharedPrefsStoreKey<T>(
    id: String,
    uuid: String? = null,
    clazz: Class<T>
): StoreKey<T>(id, uuid, StoreKind.SHARED_PREFERENCES, clazz)