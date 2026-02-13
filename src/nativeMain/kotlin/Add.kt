@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import add.addCFun

@CName("add_c_name")
fun addKtFun(a: Int, b: Int): Int {
    return addCFun(a, b)
}

fun main() {
    println("Kotlin/Native library loaded")
}
