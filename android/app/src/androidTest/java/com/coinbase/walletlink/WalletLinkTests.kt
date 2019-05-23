package com.coinbase.walletlink

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Base64
import com.coinbase.crypto.algorithms.AES256GCM
import com.coinbase.walletlink.extensions.base64EncodedString
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class WalletLinkTests {
    @Test
    fun testWalletLinkConnect() {
        val appContext = InstrumentationRegistry.getTargetContext()
        val walletLink = WalletLink(url = "ws://10.0.2.2:3003/rpc", context = appContext)
        val latch = CountDownLatch(1)

        GlobalScope.launch {
            walletLink.link(
                sessionId = "c9db0147e942b2675045e3f61b247692",
                secret = "29115acb7e001f1092e97552471c1116"
            )
            .subscribe(
                {
                    latch.countDown()
                },
                {
                    Assert.fail("Unable to connect due to $it")
                    latch.countDown()
                }
            )
        }

        latch.await()
    }

    @Test
    fun encryptionDecryption() {
        val data = "Needs encryption".toByteArray()
        val key = "c9db0147e942b2675045e3f61b247692".toByteArray()
        val iv = "123456789012".toByteArray()
        val (encryptedData, authTag) = AES256GCM.encrypt(data, key, iv)

        println(Base64.encodeToString(encryptedData, Base64.NO_WRAP))
        val decryptedData = AES256GCM.decrypt(encryptedData, key, iv, authTag)

        Assert.assertEquals(data.base64EncodedString(), decryptedData.base64EncodedString())
    }
}