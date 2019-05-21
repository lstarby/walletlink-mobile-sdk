package com.coinbase.crypto.algorithms

import com.coinbase.crypto.exceptions.EncryptionException
import com.coinbase.walletlink.utils.ByteArrayUtils

// Utility used to encrypt/decrypt using AES-256 GCM
class AES256GCM {
    companion object {
        private const val AES256_ALGORITHM = "PBKDF2WithHmacSHA256"

        /**
         * Encrypt data using using AES-256 GCM
         *
         * @param data Data to encrypt
         * @param key Secret used to encrypt the data
         * @param iv Initialization vector. Acts as a salt
         *
         * @return The encrypted data
         * @throws `EncryptionException.invalidAES256GCMData` if unable to encrypt data
         */
        @Throws(EncryptionException.InvalidAES256GCMData::class)
        fun encrypt(data: ByteArray, key: ByteArray, iv: ByteArray): Pair<ByteArray, ByteArray> {
            return Pair(ByteArrayUtils.randomBytes(1), ByteArrayUtils.randomBytes(1))

            // FIXME: hish
            /*
               val keySpec = PBEKeySpec(key.toCharArray(), salt, ITERATIONS, KEY_SIZE)
               val gcmSpec = GCMParameterSpec(AUTH_TAG_SIZE, iv)
               val secretKey = SecretKeySpec(SecretKeyFactory.getInstance(ALGORITHM).generateSecret(keySpec).encoded, "AES")
               val cipher = Cipher.getInstance(CIPHER_DERIVATION_PATH)
               cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
               val cipherTextWithAuthTag = cipher.doFinal(mnemonic.toByteArray())
               val cipherText = cipherTextWithAuthTag.copyOfRange(0, cipherTextWithAuthTag.size - (AUTH_TAG_SIZE / Byte.SIZE_BITS))
               val authTag = cipherTextWithAuthTag.copyOfRange(cipherTextWithAuthTag.size - (AUTH_TAG_SIZE / Byte.SIZE_BITS), cipherTextWithAuthTag.size)
               val timestamp = simpleDateFormat.format(Date())

               val backupPayload = BackupPayload(
                   salt = salt.toBase64String(),
                   iv = iv.toBase64String(),
                   cipherText = cipherText.toBase64String(),
                   authTag = authTag.toBase64String(),
                   hash = hashMnemonic(mnemonic),
                   timestamp = timestamp,
                   username = username)

               return BackupPayloadJsonAdapter(moshi).toJson(backupPayload)
               */
        }
    }
}