package com.coinbase.walletlink.concurrency

class BlockOperation(private val block: () -> Unit) : Operation {
    override fun main() {
        block()
    }
}