@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import add.add
import multiply.multiplyNumbers

@CName("subtract_numbers")
fun subtractNumbers(a: Int, b: Int): Int {
    return a - b
}

@CName("add_numbers")
fun addNumbers(a: Int, b: Int): Int {
    return add.add(a, b)
}

@CName("multiply_numbers")
fun multiplyNumbersWrapper(a: Int, b: Int): Int {
    return multiplyNumbers(a, b)
}

fun main() {
    println("Kotlin/Native library loaded")
}