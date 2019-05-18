package com.coinbase.walletlink.extension

import io.reactivex.Observable
import io.reactivex.Single

fun <T> Observable<T>.takeSingle(): Single<T> {
    return this.take(1).singleOrError()
}

fun <T> Observable<T>.logError(): Observable<T> {
    return doOnError { android.util.Log.e("walletlink", it.localizedMessage) }
}