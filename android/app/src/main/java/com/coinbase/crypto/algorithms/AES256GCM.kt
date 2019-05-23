package com.coinbase.crypto.algorithms

import com.coinbase.crypto.exceptions.EncryptionException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// Utility used to encrypt/decrypt using AES-256 GCM
class AES256GCM {
    companion object {
        private const val AUTH_TAG_SIZE = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        /**
         * Encrypt data using using AES-256 GCM
         *
         * @param data Data to encrypt
         * @param key Secret used to encrypt the data
         * @param iv Initialization vector. Acts as a salt
         *
         * @return A pair with encrypted data and authTag
         * @throws `EncryptionException.invalidAES256GCMData` if unable to encrypt data
         */
        @Throws(EncryptionException.InvalidAES256GCMData::class)
        fun encrypt(data: ByteArray, key: ByteArray, iv: ByteArray): Pair<ByteArray, ByteArray> {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val paramSpec = GCMParameterSpec(AUTH_TAG_SIZE, iv)
            val keySpec = SecretKeySpec(key, "AES")

            // FIXME: hish - do I need to catch runtime exceptions?

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec)
            val cipherBytes = cipher.doFinal(data)
            val ciphertextEndIndex = cipherBytes.size - (AUTH_TAG_SIZE / Byte.SIZE_BITS)
            val cipherTextBytes = cipherBytes.copyOfRange(0, ciphertextEndIndex)
            val authTagBytes = cipherBytes.copyOfRange(ciphertextEndIndex, cipherBytes.size)

            return Pair(cipherTextBytes, authTagBytes)
        }

        @Throws(EncryptionException.InvalidAES256GCMData::class)
        fun encrypt(data: ByteArray, secretKey: SecretKey): Triple<ByteArray, ByteArray, ByteArray> {
            val cipher = Cipher.getInstance(TRANSFORMATION)

            // FIXME: hish - do I need to catch runtime exceptions?

            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val cipherBytes = cipher.doFinal(data)
            val ciphertextEndIndex = cipherBytes.size - (AUTH_TAG_SIZE / Byte.SIZE_BITS)
            val cipherTextBytes = cipherBytes.copyOfRange(0, ciphertextEndIndex)
            val authTagBytes = cipherBytes.copyOfRange(ciphertextEndIndex, cipherBytes.size)

            return Triple(cipher.iv, authTagBytes, cipherTextBytes)
        }

        /**
         * Decrypt data using using AES-256 GCM
         *
         * @param data Data to decrypt
         * @param key Secret used to encrypt the data
         * @param iv Initialization vector. Acts as a salt
         * @param authTag authentication tag. Used to verify the integrity of the data
         *
         * @return The decrypted data
         * @throws`EncryptionError.invalidAES256GCMData` if unable to decrypt data
         */
        @Throws(IllegalBlockSizeException::class, BadPaddingException::class)
        fun decrypt(data: ByteArray, key: ByteArray, iv: ByteArray, authTag: ByteArray): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val paramSpec = GCMParameterSpec(AUTH_TAG_SIZE, iv)
            val keySpec = SecretKeySpec(key, "AES")
            val encryptedData = data + authTag

            cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec)

            return cipher.doFinal(encryptedData)
        }

        @Throws(IllegalBlockSizeException::class, BadPaddingException::class)
        fun decrypt(data: ByteArray, secretKey: SecretKey, iv: ByteArray, authTag: ByteArray): ByteArray {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val paramSpec = GCMParameterSpec(AUTH_TAG_SIZE, iv)
            val encryptedData = data + authTag

            cipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec)

            return cipher.doFinal(encryptedData)
        }
    }
}