package com.coinbase.walletlink.extensions

import io.reactivex.Observable
import io.reactivex.Single

// Converts an `Observable` to a `Single`
fun <T> Observable<T>.takeSingle(): Single<T> {
    return this.take(1).singleOrError()
}

// Log any caught exception triggered inside an `Observable`
fun <T> Observable<T>.logError(): Observable<T> {
    return doOnError { android.util.Log.e("walletlink", it.localizedMessage) }
}
