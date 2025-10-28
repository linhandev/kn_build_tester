@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

fun addNumbers(a: Int, b: Int): Int {
    return a + b
}

@CName("bizB_negate")
fun negate(x: Int): Int {
    // Keep bizB self-contained (no dependency on bizA)
    return -x
}