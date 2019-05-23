package com.coinbase.walletlink.extensions

import android.util.Base64
import com.coinbase.crypto.algorithms.AES256GCM
import com.coinbase.walletlink.exceptions.WalletLinkExeception

// Convert ByteArray to base64 String
fun ByteArray.base64EncodedString(): String {
    return Base64.encodeToString(this, Base64.NO_WRAP)
}

/**
 * Decrypt bytes using AES256GCM algorithm for given secret, iv and authTag
 *
 * @param key Secret used to encrypt the data
 * @param iv Initialization vector. Acts as a salt
 * @param authTag authentication tag. Used to verify the integrity of the data
 *
 * @return Decrypted string
 * @throws `WalletLinkError.unableTDecryptData` if unable to encrypt data
 */
@Throws(WalletLinkExeception.unableToDecryptData::class)
fun ByteArray.decryptUsingAES256GCM(key: ByteArray, iv: ByteArray, authTag: ByteArray): String {
    try {
        return AES256GCM.decrypt(this, key, iv, authTag).toString(Charsets.UTF_8)
    } catch (e: Exception) {
        throw WalletLinkExeception.unableToDecryptData()
    }
}