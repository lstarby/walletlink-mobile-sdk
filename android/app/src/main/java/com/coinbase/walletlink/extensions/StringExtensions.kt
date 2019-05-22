package com.coinbase.walletlink.extensions

import android.util.Base64
import com.coinbase.crypto.algorithms.AES256GCM
import com.coinbase.walletlink.exceptions.WalletLinkExeception
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Encrypt string using AES256 algorithm for given secret and iv
 *
 *  @param secret Secret used to encrypt the data
 *  @param iv Initialization vector. Acts as a salt
 *
 * @return The encrypted data
 * @throws A `WalletLinkError.unableToEncryptData` if unable to encrypt data
 */
@Throws(WalletLinkExeception.unableToEncryptData::class)
fun String.encryptUsingAES256GCM(secret: String, iv: ByteArray): String {
    try {
        val secretData = secret.base64EncodedByteArray()
        val dataToEncrypt = this.toByteArray()
        val result = AES256GCM.encrypt(dataToEncrypt, secretData, iv)
        val combinedByteArray = iv + result.first

        return combinedByteArray.base64EncodedString()
    } catch (err: IllegalAccessException) {
        throw WalletLinkExeception.unableToEncryptData()
    }
}

/**
 * Convert String to ByteArray
 *
 * @throws A `IllegalArgumentException` if unable to convert to base64
 */
@Throws(IllegalArgumentException::class)
fun String.base64EncodedByteArray(): ByteArray {
    return Base64.decode(this, Base64.NO_WRAP)
}

// Convert JSON string to Map<String, Any>
fun String.jsonMap(): Map<String, Any>? {
    val moshi = Moshi.Builder().build() // FIXME: hish - shared?
    val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    val adapter = moshi.adapter<Map<String, Any>>(type)
    return adapter.fromJson(this)
}