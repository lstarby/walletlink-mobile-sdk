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
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
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
            // FIXME: hish - pass regular map and internally have kotlin convert it to ConcurrentHashMap
            val metadata = ConcurrentHashMap<ClientMetadataKey, String>()
            metadata[ClientMetadataKey.EthereumAddress] = "0x03F6f282373900C2F6CE53B5A9f595b92aC5f5E5"

            walletLink.link(
                sessionId = "54075a65ae1ee3f29b1c562bd4688c94",
                userId = "1",
                secret = "051b4d61a7cf0258e736f47624d118e3807282a7730aa02be480aa4a4ab444b0",
                url = URL("https://walletlink.herokuapp.com/rpc"),
                metadata = metadata
            )
            .subscribe(
                {
                    println("wallet link connected!!")
                    latch.countDown()
                },
                {
                    Assert.fail("Unable to connect due to $it")
                    latch.countDown()
                }
            )
//
//            walletLink.requestsObservable
//                .subscribe {
//                    println(it)
//                  //  latch.countDown()
//                }
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

//    @Test
//    fun testAdapter() {
//        val moshi = Moshi.Builder()
//            .add(URL::class.java, URLAdapterAdapter())
//            .add(BigDecimal::class.java, BigDecimalAdapterAdapter())
//            .add(BigInteger::class.java, BigIntegerAdapterAdapter())
//         //   .add(Web3RequestDTOAdapterAdapter())
//            .build()
//
//        val type = Types.newParameterizedType(Web3RequestDTO::class.java, String::class.java, Any::class.java)
//        val adapter = JSON.moshi.adapter<Map<String, Any>>(type)
//
//        val adapter: JsonAdapter<Web3RequestDTO<*>> = moshi.adapter(Web3RequestDTO::class.java)
//        val movie = adapter.fromJson("poo")
//    }
}
