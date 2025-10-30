@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

@kotlinx.cinterop.ExperimentalForeignApi
@CName("subtract_numbers")
fun subtractNumbers(a: Int, b: Int): Int {
    return sanTest.implementedFunction()
}
