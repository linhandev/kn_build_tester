@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

@CName("bizA_times_two")
fun timesTwo(x: Int): Int {
    return addNumbers(x, x)
}
