package io.github.kotlin.fibonacci

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test running ON the device/emulator, covering the library's own androidMain code.
 * uncalledUtility() is intentionally NOT called — coverage must flag it.
 */
@RunWith(AndroidJUnit4::class)
class CalculatorDeviceTest {

    @Test
    fun classifyOnDevice() {
        assertEquals("small", Calculator.classify(7))
        assertEquals("large", Calculator.classify(100))
    }

    @Test
    fun isPrimeOnDevice() {
        assertTrue(Calculator.isPrime(7))
    }

    @Test
    fun fibiOnDevice() {
        assertEquals(firstElement + secondElement, generateFibi().take(3).last())
    }
}
