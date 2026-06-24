package io.github.kotlin.fibonacci.app

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.kotlin.fibonacci.Calculator
import io.github.kotlin.fibonacci.firstElement
import io.github.kotlin.fibonacci.generateFibi
import io.github.kotlin.fibonacci.secondElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test running ON the device/emulator. Exercises the library's Calculator so that
 * on-device coverage (enableAndroidTestCoverage) records it. uncalledUtility() is intentionally
 * not called — the coverage report must flag it as uncovered.
 */
@RunWith(AndroidJUnit4::class)
class CalculatorDeviceTest {

    @Test
    fun launchActivityAndExerciseLibrary() {
        ActivityScenario.launch(MainActivity::class.java).use { /* forces onCreate → Calculator calls */ }
    }

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
