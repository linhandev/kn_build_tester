@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

@CName("subtract_numbers")
fun subtractNumbers(a: Int, b: Int): Int {
    return a - b
}

fun main() {
    println("Kotlin/Native library loaded")
}