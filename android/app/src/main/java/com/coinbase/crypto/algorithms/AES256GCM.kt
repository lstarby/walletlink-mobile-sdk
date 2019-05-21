package com.coinbase.crypto.algorithms

import com.coinbase.crypto.exceptions.EncryptionException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// Utility used to encrypt/decrypt using AES-256 GCM
class AES256GCM {
    companion object {
        private const val AUTH_TAG_SIZE = 128

        /**
         * Encrypt data using using AES-256 GCM
         *
         * @param data Data to encrypt
         * @param key Secret used to encrypt the data
         * @param iv Initialization vector. Acts as a salt
         *
         * @return A pair with encrypted data and authTag respectively
         * @throws `EncryptionException.invalidAES256GCMData` if unable to encrypt data
         */
        @Throws(EncryptionException.InvalidAES256GCMData::class)
        fun encrypt(data: ByteArray, key: ByteArray, iv: ByteArray): Pair<ByteArray, ByteArray> {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val paramSpec = GCMParameterSpec(AUTH_TAG_SIZE, iv)
            val keySpec = SecretKeySpec(key, "AES")

            // FIXME: hish - do I need to catch runtime exceptions?

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec)
            val cipherBytes = cipher.doFinal(data)
            val cipherTextBytes = cipherBytes.copyOfRange(
                0,
                cipherBytes.size - (AUTH_TAG_SIZE / Byte.SIZE_BITS)
            )

            val authTagBytes = cipherBytes.copyOfRange(
                cipherBytes.size - (AUTH_TAG_SIZE / Byte.SIZE_BITS),
                cipherBytes.size
            )

            return Pair(cipherTextBytes, authTagBytes)
        }
    }
}