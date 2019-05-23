package com.coinbase.walletlink.extensions

import android.util.Base64
import com.coinbase.crypto.algorithms.AES256GCM
import com.coinbase.walletlink.exceptions.WalletLinkExeception
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * Encrypt string using AES256GCM algorithm for given secret and iv
 *
 *  @param key Secret used to encrypt the data
 *  @param iv Initialization vector. Acts as a salt
 *
 * @return The encrypted data
 * @throws `WalletLinkError.unableToEncryptData` if unable to encrypt data
 */
@Throws(WalletLinkExeception.unableToEncryptData::class)
fun String.encryptUsingAES256GCM(key: ByteArray, iv: ByteArray): String {
    try {
        val dataToEncrypt = this.toByteArray()
        val result = AES256GCM.encrypt(dataToEncrypt, key, iv)
        val combinedByteArray = iv + result.second + result.first

        return combinedByteArray.base64EncodedString()
    } catch (err: IllegalAccessException) {
        throw WalletLinkExeception.unableToEncryptData()
    }
}

// Helper function to allow String `secret`. See function above for details
@Throws(WalletLinkExeception.unableToEncryptData::class)
fun String.encryptUsingAES256GCM(key: String, iv: ByteArray): String {
    return encryptUsingAES256GCM(key.toByteArray(), iv)
}

/**
 * Parse AES256 GCM payload i.e. IV (12 bytes) + Auth Tag (16 bytes) + CiperText (rest of bytes)
 *
 * @returns Triple containing UV + Auth Tag + Cipher text
 */
fun String.parseAES256GMPayload(): Triple<ByteArray, ByteArray, ByteArray>? {
    val encryptedData = this.base64DecodedByteArray()
    val ivEndIndex = 12
    val authTagEndIndex = ivEndIndex + 16
    val iv = encryptedData.copyOfRange(0, ivEndIndex)
    val authTag = encryptedData.copyOfRange(ivEndIndex, authTagEndIndex)
    val cipherText = encryptedData.copyOfRange(authTagEndIndex, encryptedData.size)

    return Triple(iv, authTag, cipherText)
}

/**
 * Convert String to ByteArray
 *
 * @throws `IllegalArgumentException` if unable to convert to base64
 */
@Throws(IllegalArgumentException::class)
fun String.base64DecodedByteArray(): ByteArray {
    return Base64.decode(this, Base64.NO_WRAP)
}

// Convert JSON string to Map<String, Any>
fun String.jsonMap(): Map<String, Any>? {
    val moshi = Moshi.Builder().build() // FIXME: hish - shared?
    val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    val adapter = moshi.adapter<Map<String, Any>>(type)
    return adapter.fromJson(this)
}