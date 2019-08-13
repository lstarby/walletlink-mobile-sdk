package com.coinbase.walletlink

import com.coinbase.wallet.core.interfaces.JsonSerializable
import com.coinbase.walletlink.dtos.JoinSessionMessageDTO
import org.junit.Assert
import org.junit.Test

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

    private fun convertToJson(serializable: JsonSerializable): String {
        return serializable.asJsonString()
    }
}
