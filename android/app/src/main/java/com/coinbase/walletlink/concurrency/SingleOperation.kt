package com.coinbase.walletlink.concurrency

import io.reactivex.Single
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch

class SingleOperation<T>(private val single: Single<T>) : Operation {
    var result: Result<T>? = null
        private set

    override fun main() {
        val countDownLatch = CountDownLatch(1)

        GlobalScope.launch {
            single.subscribe({
                result = Result(response = it)
                countDownLatch.countDown()
            }, {
                result = Result(throwable = null)
                countDownLatch.countDown()
            })
        }

        countDownLatch.await()
    }
}
