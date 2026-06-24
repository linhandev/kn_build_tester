package io.github.kotlin.fibonacci.app

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import io.github.kotlin.fibonacci.Calculator
import io.github.kotlin.fibonacci.generateFibi

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        // Exercise the library code so it shows up in on-device coverage.
        val cls = Calculator.classify(7)       // -> "small"
        val prime = Calculator.isPrime(7)      // -> true
        val fib = generateFibi().take(5).joinToString()
        // uncalledUtility is deliberately NOT called here — coverage must flag it.
        tv.text = "classify(7)=$cls, isPrime(7)=$prime, fib=$fib"
        setContentView(tv)
    }
}
