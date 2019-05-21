package com.coinbase.walletlink

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.coinbase.store.Store
import com.coinbase.store.models.StoreKey
import com.coinbase.store.models.StoreKind
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

data class MockComplexObject(val name: String, val age: Int, val wallets: List<String>)

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.coinbase.walletlink", appContext.packageName)
    }

    @Test
    fun testStore() {
        val appContext = InstrumentationRegistry.getTargetContext()
        val store = Store(appContext)
        val stringKey = StoreKey("string_key", "id", StoreKind.SHARED_PREFERENCES, String::class.java)
        val boolKey = StoreKey("bool_key", "id", StoreKind.SHARED_PREFERENCES, Boolean::class.java)
        val complexObjectKey = StoreKey(
            "complex_object",
            null,
            StoreKind.SHARED_PREFERENCES,
            MockComplexObject::class.java
        )

        val expected = "Hello Android CBStore"
        val expectedComplex = MockComplexObject(name = "hish", age = 37, wallets = listOf("hello", "world"))

        store.set(stringKey, expected)
        store.set(boolKey, false)
        store.set(complexObjectKey, expectedComplex)
        store.set(TestKeys.computedKey(id = "random"), "hello")

        store.set(TestKeys.activeUser, "random")

        assertEquals(expected, store.get(stringKey))
        assertEquals(false, store.get(boolKey))
        assertEquals(expectedComplex, store.get(complexObjectKey))
        assertEquals("hello", store.get(TestKeys.computedKey(id = "random")))
    }

    @Test
    fun testMemory() {
        val expected = "Memory string goes here"
        val appContext = InstrumentationRegistry.getTargetContext()
        val store = Store(appContext)

        store.set(TestKeys.memoryString, expected)

        assertEquals(expected, store.get(TestKeys.memoryString))
    }

    @Test
    fun testObserver() {
        val expected = "Testing observer"
        val appContext = InstrumentationRegistry.getTargetContext()
        val store = Store(appContext)
        val latchDown = CountDownLatch(1)
        var actual = ""

        GlobalScope.launch {
            store.observe(TestKeys.memoryString)
                .filter { it != null }
                .timeout(6, TimeUnit.SECONDS)
                .subscribe({
                    actual = it.element ?: throw AssertionError("No element found")
                    latchDown.countDown()
                }, { latchDown.countDown() })
        }

        store.set(TestKeys.memoryString, expected)
        latchDown.await()

        assertEquals(expected, actual)
    }
}

class TestKeys {
    companion object {
        val activeUser = StoreKey("computedKeyX", null, StoreKind.SHARED_PREFERENCES, String::class.java)

        fun computedKey(id: String): StoreKey<String> {
            return StoreKey("computedKey", id, StoreKind.SHARED_PREFERENCES, String::class.java)
        }

        val memoryString = StoreKey(id = "memory_string", kind = StoreKind.MEMORY, clazz = String::class.java)
    }
}
