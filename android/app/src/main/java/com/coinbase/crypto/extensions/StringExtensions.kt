package com.coinbase.crypto.extensions

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

@Throws(NoSuchAlgorithmException::class)
fun String.sha256(): String {
    val md = MessageDigest.getInstance("SHA-256")
    md.update(this.toByteArray())
    return md.digest().toHexString()
}