package com.coinbase.walletlink

import com.coinbase.wallet.store.jsonadapters.UrlAdapter
import com.coinbase.wallet.store.utils.JSON
import com.coinbase.walletlink.dtos.JoinSessionMessageDTO
import com.coinbase.walletlink.interfaces.JsonSerializable
import com.squareup.moshi.Moshi
import org.junit.Assert
import org.junit.Test
import java.net.URL

class JsonSerializableTests {
    @Test
    fun testJsonConversion() {

        val dto = JoinSessionMessageDTO(
            id = 1234,
            sessionId = "12jdfjhkjhsf",
            sessionKey = "232348943"
        )

        val json = convertToJson(dto)
        val expected = """
            {"id":1234,"sessionId":"12jdfjhkjhsf","sessionKey":"232348943","type":"JoinSession"}
        """.trimIndent()

        Assert.assertEquals(expected, json)
    }

    fun convertToJson(serializable: JsonSerializable): String {
        return serializable.asJsonString()
    }
}

inline fun <reified T> toJsonString(instance: T): String {
    val moshi = Moshi.Builder()
        .add(URL::class.java, UrlAdapter())
        .build()

    val adapter = JSON.moshi.adapter<T>(T::class.java)
    return adapter.toJson(instance)
}
