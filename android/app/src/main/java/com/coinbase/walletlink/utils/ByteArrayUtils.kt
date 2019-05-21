package com.coinbase.walletlink.utils

import java.security.SecureRandom

class ByteArrayUtils {
    companion object {
        fun randomBytes(size: Int): ByteArray {
            return ByteArray(size).apply { SecureRandom().nextBytes(this) }
        }
    }
}
