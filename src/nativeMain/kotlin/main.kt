@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

/**
 * Simple subtraction function that can be called from C
 * @param a First number
 * @param b Second number  
 * @return Difference of a - b
 */
@CName("subtract_numbers")
fun subtractNumbers(a: Int, b: Int): Int {
    return a - b
}

fun main() {
    println("Kotlin/Native library loaded")
}