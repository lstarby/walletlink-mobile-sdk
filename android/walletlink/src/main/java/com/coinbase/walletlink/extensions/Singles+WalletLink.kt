package com.coinbase.walletlink.extensions

import io.reactivex.Single
import io.reactivex.rxkotlin.Singles

/**
 * Safe way of passing singles to be zipped or returns empty if supplied list is empty
 *
 * @param singles List of singles to zip together
 *
 * @return result or zip operator or empty list
 */
inline fun <reified T> Singles.zipOrEmpty(singles: List<Single<T>>): Single<List<T>> {
    if (singles.isEmpty()) {
        return Single.just(listOf())
    }

    return Single.zip(singles) {
        if (it.isEmpty()) {
            listOf()
        } else {
            it.filterIsInstance<T>()
        }
    }
}
