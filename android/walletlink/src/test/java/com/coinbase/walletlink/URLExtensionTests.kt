package com.coinbase.walletlink

import com.coinbase.wallet.extensions.appendingPathComponent
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URL

class URLExtensionTests {
    @Test
    fun testURLAppendPathWithoutTrailingSlash() {
        val url = URL("https://www.coinbase.com")
        val actualURL = url.appendingPathComponent("/user").appendingPathComponent("id")
        assertEquals(URL("https://www.coinbase.com/user/id").toString(), actualURL.toString())
    }

    @Test
    fun testURLAppendPathWithTrailingSlash() {
        val url = URL("https://www.coinbase.com/")
        val actualURL = url.appendingPathComponent("/user").appendingPathComponent("id")
        assertEquals(URL("https://www.coinbase.com/user/id").toString(), actualURL.toString())
    }

    @Test
    fun testURLAppendPathWithTrailingSlashAndNoPathSlash() {
        val url = URL("https://www.coinbase.com/")
        val actualURL = url.appendingPathComponent("user").appendingPathComponent("id")
        assertEquals(URL("https://www.coinbase.com/user/id").toString(), actualURL.toString())
    }

    @Test
    fun testURLAppendPathWithoutTrailingSlashAndNoPathSlash() {
        val url = URL("https://www.coinbase.com")
        val actualURL = url.appendingPathComponent("user").appendingPathComponent("id")
        assertEquals(URL("https://www.coinbase.com/user/id").toString(), actualURL.toString())
    }
}
