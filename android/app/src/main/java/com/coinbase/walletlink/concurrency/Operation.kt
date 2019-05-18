package com.coinbase.walletlink.concurrency

 // Conformers of this interface can run inside OperationQueue
interface Operation {
    // Called to run the operation from OperationQueue
    fun main()
}