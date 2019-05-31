package com.coinbase.walletlink.extensions

import com.coinbase.wallet.crypto.algorithms.AES256GCM
import com.coinbase.wallet.store.utils.JSON
import com.coinbase.walletlink.exceptions.WalletLinkException
import com.coinbase.walletlink.utils.ByteArrayUtils
import com.squareup.moshi.Types
import java.math.BigInteger

private const val hexadecimalCharacters = "0123456789abcdef"
private const val kAES256GCMIVSize = 12
private const val kAES256GCMAuthTagSize = 16

/**
 * Encrypt string using AES256GCM algorithm for given secret and iv
 *
 *  @param secret Secret used to encrypt the data
 *  @param iv Initialization vector. Acts as a salt
 *
 * @return The encrypted data
 * @throws `WalletLinkError.unableToEncryptData` if unable to encrypt data
 */
@Throws(WalletLinkException.UnableToEncryptData::class)
fun String.encryptUsingAES256GCM(secret: ByteArray, iv: ByteArray): String {
    try {
        val dataToEncrypt = toByteArray()
        val result = AES256GCM.encrypt(dataToEncrypt, secret, iv)
        val combinedByteArray = iv + result.second + result.first

        return combinedByteArray.toPrefixedHexString().strip0x()
    } catch (err: IllegalAccessException) {
        throw WalletLinkException.UnableToEncryptData
    }
}

/**
 * Helper function to allow String `secret`. See function above for details
 */
@Throws(WalletLinkException.UnableToEncryptData::class)
fun String.encryptUsingAES256GCM(secret: String, iv: ByteArray): String {
    val secretData = secret.byteArrayUsingHexEncoding() ?: throw WalletLinkException.UnableToEncryptData
    return encryptUsingAES256GCM(secretData, iv)
}

/**
 * Helper function to generate a random IV. See function above for details
 */
@Throws(WalletLinkException.UnableToEncryptData::class)
fun String.encryptUsingAES256GCM(secret: String): String {
    return encryptUsingAES256GCM(secret, ByteArrayUtils.randomBytes(kAES256GCMIVSize))
}

/**
 * Decrypt string with AES256 GCM using provided secret
 */
@Throws(WalletLinkException.UnableToDecryptData::class)
fun String.decryptUsingAES256GCM(secret: String): ByteArray {
    val encryptedData = byteArrayUsingHexEncoding() ?: throw WalletLinkException.UnableToDecryptData
    val secretData = secret.byteArrayUsingHexEncoding() ?: throw WalletLinkException.UnableToDecryptData

    if (encryptedData.size < (kAES256GCMAuthTagSize + kAES256GCMIVSize)) {
        throw WalletLinkException.UnableToDecryptData
    }

    try {
        val ivEndIndex = kAES256GCMIVSize
        val authTagEndIndex = ivEndIndex + kAES256GCMAuthTagSize
        val iv = encryptedData.copyOfRange(0, ivEndIndex)
        val authTag = encryptedData.copyOfRange(ivEndIndex, authTagEndIndex)
        val dataToDecrypt = encryptedData.copyOfRange(authTagEndIndex, encryptedData.size)

        return AES256GCM.decrypt(data = dataToDecrypt, key = secretData, iv = iv, authTag = authTag)
    } catch (err: Exception) {
        throw WalletLinkException.UnableToDecryptData
    }
}

/**
 * Strip out "0x" prefix if one exists. Otherwise, no-op
 */
fun String.strip0x(): String {
    return if (startsWith("0x")) {
        this.substring(2)
    } else {
        this
    }
}

/**
 * Convert to hex Data if possible
 */
fun String.byteArrayUsingHexEncoding(): ByteArray? {
    val str = this.strip0x().toLowerCase().let {
        if (it.length % 2 == 0) {
            it
        } else {
            "0$it"
        }
    }

    val size = str.length / 2
    val bytes = ByteArray(size)

    for (i in 0 until size) {
        val valLeft = hexadecimalCharacters.indexOf(str[i * 2])
        if (valLeft == -1) {
            return null
        }
        val valRight = hexadecimalCharacters.indexOf(str[i * 2 + 1])
        if (valRight == -1) {
            return null
        }
        bytes[i] = (valLeft * 16 + valRight).toByte()
    }
    return bytes
}

/**
 * Convert JSON string to Map<String, Any>
 */
fun String.asJsonMap(): Map<String, Any>? {
    val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    val adapter = JSON.moshi.adapter<Map<String, Any>>(type)

    return adapter.fromJson(this)
}

/**
 * Helper function to convert optional string to a big integer
 */
val String?.asBigInteger: BigInteger? get() = this?.let { BigInteger(it) }
