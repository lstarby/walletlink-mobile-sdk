package com.coinbase.walletlink.utils

import java.security.SecureRandom

class ByteArrayUtils {
    companion object {
        /**
         * Generate random bytes using given size
         *
         * @param size length of ByteArray to generate
         *
         * @return A new instance of ByteArray with random bytes
         */
        fun randomBytes(size: Int): ByteArray {
            return ByteArray(size).apply { SecureRandom().nextBytes(this) }
        }
    }
}
