package com.example.lib

// ─── Public API: re-exported by the :app module via export(project(":lib")) ───

fun libAdd(a: Int, b: Int): Int = a + b

fun libGreet(name: String): String = "Hello from lib, $name!"

class LibCalculator {
    fun multiply(a: Int, b: Int): Int = a * b
    fun divide(a: Double, b: Double): Double = if (b != 0.0) a / b else Double.NaN
}

enum class LibStatus { OK, ERROR, PENDING }

// ─── Internal API: NOT visible in C header even when the module is re-exported ───

internal fun libInternalHelper(): Int = 42

internal class LibSecret {
    fun hidden(): String = "not exported"
}
