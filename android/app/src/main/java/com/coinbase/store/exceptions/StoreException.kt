package com.coinbase.store.exceptions

class StoreException {
    class UnableToCreateObserver : RuntimeException("Unable to create a store value observer")
}