package com.coinbase.walletlink.extensions

import io.reactivex.Single

fun <T> Single<T>.asUnit(): Single<Unit> {
    return this.map { Unit }
}

fun <T> Single<T>.logError(): Single<T> {
    return doOnError { android.util.Log.e("walletlink", it.localizedMessage) }
}