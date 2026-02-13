@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import add.addcfun

@CName("add_numbers")
fun addNumbers(a: Int, b: Int): Int {
    return addcfun(a, b)
}

fun main() {
    println("Kotlin/Native library loaded")
}
