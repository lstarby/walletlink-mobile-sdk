package com.coinbase.store.encryption

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.IOException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableEntryException
import java.security.cert.CertificateException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class KeyStoreEncryption {
    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        // private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ALIAS = "com.coinbase.wallet.CBStore"

        const val PLAINTEXT_PREFIX = "plaintext/"
        const val ENCRYPTED_PREFIX = "encrypted/"
    }

    // Android KeyStore private/public key generation

    @Throws(
        KeyStoreException::class,
        IOException::class,
        NoSuchAlgorithmException::class,
        CertificateException::class,
        UnrecoverableEntryException::class
    )
    private fun getSecretKey(): SecretKey {
        // Attempt to fetch existing stored secret key from Android KeyStore
        val keyStore = KeyStore.getInstance(KEYSTORE)

        keyStore.load(null)

        val entry = keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry
        val secretKey = entry?.secretKey

        if (secretKey != null) {
            return secretKey
        }

        // At this point, no secret key is stored so generate a new one.
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE
        )

        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        keyGenerator.init(spec)

        return keyGenerator.generateKey()
    }
}