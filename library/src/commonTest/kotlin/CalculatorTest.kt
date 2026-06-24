package io.github.kotlin.fibonacci

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalculatorTest {

    @Test
    fun classify() {
        assertEquals("negative", Calculator.classify(-1))
        assertEquals("zero", Calculator.classify(0))
        assertEquals("small", Calculator.classify(5))
        assertEquals("large", Calculator.classify(100))
    }

    @Test
    fun primes() {
        assertFalse(Calculator.isPrime(0))
        assertFalse(Calculator.isPrime(1))
        assertTrue(Calculator.isPrime(2))
        assertTrue(Calculator.isPrime(7))
        assertFalse(Calculator.isPrime(9))
        // NOTE: no assertion for NaN-like edge cases — leaves branches uncovered on purpose.
    }
}
