package com.coinbase.store.models

open class StoreKey<T>(id: String, uuid: String? = null, val kind: StoreKind, val clazz: Class<T>) {
    val name: String = listOf(
        kind.name,
        id,
        uuid,
        clazz.simpleName
        )
        .filterNotNull()
        .joinToString("_")

}