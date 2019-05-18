package com.coinbase.store

class StoreException {
    class UnableToCreateObserver : RuntimeException("Unable to create a store value observer")
}