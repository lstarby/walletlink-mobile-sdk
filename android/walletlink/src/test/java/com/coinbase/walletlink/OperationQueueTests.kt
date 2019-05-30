package com.coinbase.walletlink

import com.coinbase.walletlink.concurrency.BlockOperation
import com.coinbase.walletlink.concurrency.OperationQueue
import io.reactivex.Single
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class OperationQueueTests {
    @Test
    fun testSingleThread() {
        val operationQueue = OperationQueue(1)
        val numberOfBlocks = 3
        val latch = CountDownLatch(numberOfBlocks)
        val totalFinished = AtomicInteger()
        val millis = measureTimeMillis {
            for (i in 0 until numberOfBlocks) {
                val operation = BlockOperation {
                    Thread.sleep(1000)
                    totalFinished.incrementAndGet()
                    latch.countDown()
                }

                operationQueue.add(operation)
            }

            latch.await()
        }

        println("testMultipleThread ${millis}ms")
        Assert.assertEquals(numberOfBlocks, totalFinished.get())
        Assert.assertTrue(millis < 3500)
    }

    @Test
    fun testMultipleThread() {
        val operationQueue = OperationQueue(100)
        val numberOfBlocks = 10
        val latch = CountDownLatch(numberOfBlocks)
        val totalFinished = AtomicInteger()
        val millis = measureTimeMillis {
            for (i in 0 until numberOfBlocks) {
                val operation = BlockOperation {
                    Thread.sleep(1000)
                    totalFinished.incrementAndGet()
                    latch.countDown()
                }

                operationQueue.add(operation)
            }

            latch.await()
        }

        println("testMultipleThread ${millis}ms")
        Assert.assertEquals(numberOfBlocks, totalFinished.get())
        Assert.assertTrue(millis < 1500)
    }

    @Test
    fun testSingleOperation() {
        val operationQueue = OperationQueue(1)
        val single = Single.just(3)
            .delay(1, TimeUnit.SECONDS)

        val operation = operationQueue.addSingle(single)
        var completed = false
        val latch = CountDownLatch(1)

        operation.subscribe({
            completed = true
            latch.countDown()
        }, {
            completed = false
            latch.countDown()
        })

        latch.await()
        Assert.assertEquals(true, completed)
    }

    @Test
    fun testMultipleConcurrentSingleOperation() {
        val operationQueue = OperationQueue(10)
        val numberOfSingles = 10
        val latch = CountDownLatch(numberOfSingles)
        val totalFinished = AtomicInteger()
        val single = Single.just(3)
            .delay(1, TimeUnit.SECONDS)

        val millis = measureTimeMillis {
            for (i in 0 until numberOfSingles) {
                val operation = operationQueue.addSingle(single)
                operation.subscribe({
                    totalFinished.incrementAndGet()
                    latch.countDown()
                }, {
                    latch.countDown()
                })
            }

            latch.await()
        }

        println("testMultipleSingleOperation ${millis}ms")
        Assert.assertEquals(numberOfSingles, totalFinished.get())
        Assert.assertTrue(millis < 1500)
    }

    @Test
    fun testMultipleSerialSingleOperation() {
        val operationQueue = OperationQueue(1)
        val numberOfSingles = 3
        val latch = CountDownLatch(numberOfSingles)
        val totalFinished = AtomicInteger()
        val single = Single.just(3)
            .delay(1, TimeUnit.SECONDS)

        val millis = measureTimeMillis {
            for (i in 0 until numberOfSingles) {
                val operation = operationQueue.addSingle(single)
                operation.subscribe({
                    totalFinished.incrementAndGet()
                    latch.countDown()
                }, {
                    latch.countDown()
                })
            }

            latch.await()
        }

        println("testMultipleSingleOperation ${millis}ms")
        Assert.assertEquals(numberOfSingles, totalFinished.get())
        Assert.assertTrue(millis < 3500)
    }
}
