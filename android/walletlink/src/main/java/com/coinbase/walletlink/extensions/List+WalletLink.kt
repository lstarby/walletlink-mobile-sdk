package com.coinbase.walletlink.extensions

inline fun <reified K, reified V, reified E> List<E>.reduceIntoMap(
    map: HashMap<K, V> = HashMap(),
    closure: (HashMap<K, V>, E) -> Unit
): Map<K, V> {
    forEach { closure(map, it) }

    return map
}
