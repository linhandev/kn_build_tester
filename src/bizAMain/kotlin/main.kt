@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

fun subtractNumbers(a: Int, b: Int): Int {
    return a - b
}

@CName("bizA_times_two")
fun timesTwo(x: Int): Int {
    return addNumbers(x, x)
}