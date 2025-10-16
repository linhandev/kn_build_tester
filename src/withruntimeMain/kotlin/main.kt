@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

@CName("with_add_numbers")
fun addNumbers(a: Int, b: Int): Int {
    return a + b
}
