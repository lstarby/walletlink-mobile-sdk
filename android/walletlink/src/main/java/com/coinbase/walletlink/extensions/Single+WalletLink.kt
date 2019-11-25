// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.coinbase.walletlink.extensions

import io.reactivex.Single
import timber.log.Timber

/**
 * Log any caught exception triggered inside an [Single]
 *
 * @return The original single
 */
internal fun <T> Single<T>.logError(msg: String? = null): Single<T> = doOnError {
    Timber.e(it, "WalletLink exception $msg ${it.localizedMessage}".trim())
}
