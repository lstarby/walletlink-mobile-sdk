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
