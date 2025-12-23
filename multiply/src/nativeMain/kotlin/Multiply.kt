@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package multiply

import multiply.multiply

@CName("multiply_numbers")
fun multiplyNumbers(a: Int, b: Int): Int {
    return multiply.multiply(a, b)
}
