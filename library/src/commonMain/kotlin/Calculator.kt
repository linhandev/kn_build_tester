package io.github.kotlin.fibonacci

/**
 * Pure-logic utility with branches, shared across all targets.
 * The `isNaN` branch is intentionally NOT covered by tests, so coverage reports
 * can visibly demonstrate "uncovered code" — proving the reports are real.
 */
object Calculator {

    fun classify(n: Int): String = when {
        n < 0 -> "negative"
        n == 0 -> "zero"
        n < 10 -> "small"
        else -> "large"
    }

    /** Returns true if n is prime (n >= 2). */
    fun isPrime(n: Int): Boolean {
        if (n < 2) return false
        var d = 2
        while (d * d <= n) {
            if (n % d == 0) return false
            d++
        }
        return true
    }
}
