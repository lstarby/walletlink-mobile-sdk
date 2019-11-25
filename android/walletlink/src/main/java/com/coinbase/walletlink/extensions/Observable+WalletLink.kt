package com.coinbase.walletlink.extensions

import io.reactivex.Observable
import timber.log.Timber


/**
 * Log any caught exception triggered inside an [Observable]
 *
 * @return The original observable
 */
fun <T> Observable<T>.logError(msg: String? = null): Observable<T> = doOnError {
    Timber.e(it, "WalletLink exception $msg ${it.localizedMessage}".trim())
}
