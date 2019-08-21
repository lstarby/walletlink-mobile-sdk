// Copyright (c) 2018-2019 Coinbase, Inc. <https://coinbase.com/>
// Licensed under the Apache License, version 2.0

package com.coinbase.walletlink

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coinbase.wallet.core.extensions.base64EncodedString
import com.coinbase.wallet.crypto.ciphers.AES256GCM
import com.coinbase.wallet.http.connectivity.Internet
import com.coinbase.walletlink.models.ClientMetadataKey
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.net.URL
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class WalletLinkTests {
    @Test
    fun testWalletLinkConnect() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val intentFilter = IntentFilter().apply { addAction(ConnectivityManager.CONNECTIVITY_ACTION) }

        appContext.registerReceiver(Internet, intentFilter)
        Internet.startMonitoring()

        val walletLink = WalletLink(
            notificationUrl = URL("https://walletlink.herokuapp.com"),
            context = appContext
        )

        val latch = CountDownLatch(1)

        GlobalScope.launch {
            val metadata = mutableMapOf<ClientMetadataKey, String>()
            metadata[ClientMetadataKey.EthereumAddress] = "0x03F6f282373900C2F6CE53B5A9f595b92aC5f5E5"

            walletLink.link(
                sessionId = "bcb17224553554b53053d70cc6d05cbb",
                userId = "1",
                secret = "d2a4092708e194c850715682ee862b0a767f5a268637649aae0a4ea0eadb216f",
                url = URL("https://www.walletlink.org"),
                metadata = metadata
            )
            .subscribe(
                {
                    Timber.i("wallet link connected!!")
                    latch.countDown()
                },
                {
                    Assert.fail("Unable to connect due to $it")
                    latch.countDown()
                }
            )
        }

        val requests = walletLink.requestsObservable.blockingFirst()
        Timber.i("walletlink requests $requests")

        latch.await()
    }

    @Test
    fun encryptionDecryption() {
        val data = "Needs encryption".toByteArray()
        val key = "c9db0147e942b2675045e3f61b247692".toByteArray()
        val iv = "123456789012".toByteArray()
        val (encryptedData, authTag) = AES256GCM.encrypt(data, key, iv)

        Timber.i(Base64.encodeToString(encryptedData, Base64.NO_WRAP))
        val decryptedData = AES256GCM.decrypt(encryptedData, key, iv, authTag)

        Assert.assertEquals(data.base64EncodedString(), decryptedData.base64EncodedString())
    }
}
