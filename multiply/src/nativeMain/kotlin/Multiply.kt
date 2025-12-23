@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package multiply

import multiplyciopkg.multiplycfun

@CName("multiply_numbers")
fun multiplyNumbers(a: Int, b: Int): Int {
    return multiplycfun(a, b)
}
