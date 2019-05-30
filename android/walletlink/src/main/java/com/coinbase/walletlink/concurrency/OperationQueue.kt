package com.coinbase.walletlink.concurrency

import com.coinbase.walletlink.exceptions.OperationException
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

/**
 * A simple ThreadPool. Restricts the number of operations based on the provided thread pool size
 *
 * @param maxConcurrentThreads max number of concurrent threads to execute
 */
class OperationQueue(private val maxConcurrentThreads: Int) {
    private val context = Executors.newFixedThreadPool(maxConcurrentThreads).asCoroutineDispatcher()

    /**
     * Add a new operation to the thread pool
     *
     * @param operation Operation to execute within the thread pool
     */
    fun add(operation: Operation): Job {
        return GlobalScope.launch(context) { operation.main() }
    }

    fun <T> addSingle(single: Single<T>): Single<T> {
        return Single.create<T> { emitter ->
            runBlocking {
                val singleOperation = SingleOperation(single)
                val job = add(operation = singleOperation)

                job.join()

                val result = singleOperation.result?.response
                val error = singleOperation.result?.throwable

                when {
                    result != null -> emitter.onSuccess(result)
                    error != null -> emitter.onError(error)
                    else -> emitter.onError(OperationException.NoResult())
                }
            }
        }
        .subscribeOn(Schedulers.io())
    }
}
