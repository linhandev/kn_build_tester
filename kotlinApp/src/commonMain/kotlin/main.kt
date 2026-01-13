@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

@CName("subtract_numbers")
fun subtractNumbers(a: Int, b: Int): Int {
    return a - b
}

expect fun runGcovTest()

@CName("run_gcov_test")
fun runGcovTestWrapper() {
    runGcovTest()
}

fun main() {
    println("Kotlin/Native library loaded")
}