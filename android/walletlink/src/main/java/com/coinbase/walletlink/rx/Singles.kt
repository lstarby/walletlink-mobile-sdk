package com.coinbase.walletlink.rx

import io.reactivex.Single

class Singles<T> {
    companion object {
        inline fun <reified T> zip(singles: List<Single<T>>): Single<List<T>> {
            if (singles.isEmpty()) {
                return Single.just(listOf())
            }

            return Single.zip(singles) { it.filterIsInstance<T>() }
        }
    }
}
