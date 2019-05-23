package com.coinbase.crypto.extensions

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

// Hash the string using sha256
@Throws(NoSuchAlgorithmException::class)
fun String.sha256(): String {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(this.toByteArray())
    return md.digest().toHexString()
}