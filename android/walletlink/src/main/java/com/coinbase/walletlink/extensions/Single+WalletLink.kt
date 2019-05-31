package com.coinbase.walletlink.extensions

import io.reactivex.Single

/**
 * Converts any Single<T> to a Single<Unit>
 */
fun <T> Single<T>.asUnit(): Single<Unit> {
    return this.map { Unit }
}

/**
 * Log any caught exception triggered inside an `Single`
 */
fun <T> Single<T>.logError(): Single<T> {
    return doOnError { android.util.Log.e("walletlink", "error ${it.localizedMessage}") }
}
