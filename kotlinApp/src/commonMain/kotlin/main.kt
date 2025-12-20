@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

@CName("subtract_numbers")
fun subtractNumbers(a: Int, b: Int): Int {
    return a - b
}

expect fun runAsanTest()

@CName("run_asan_test")
fun runAsanTestWrapper() {
    println("Kotlin: runAsanTestWrapper called")
    runAsanTest()
}

fun main() {
    println("Kotlin/Native library loaded")
}