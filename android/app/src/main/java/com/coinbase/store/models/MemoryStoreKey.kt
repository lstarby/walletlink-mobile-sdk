package com.coinbase.store.models

class MemoryStoreKey<T>(
    id: String,
    uuid: String? = null,
    clazz: Class<T>
) : StoreKey<T>(id, uuid, StoreKind.MEMORY, clazz)