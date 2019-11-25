// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.coinbase.walletlink.extensions

import io.reactivex.Observable
import timber.log.Timber

/**
 * Log any caught exception triggered inside an [Observable]
 *
 * @return The original observable
 */
internal fun <T> Observable<T>.logError(msg: String? = null): Observable<T> = doOnError {
    Timber.e(it, "WalletLink exception $msg ${it.localizedMessage}".trim())
}
