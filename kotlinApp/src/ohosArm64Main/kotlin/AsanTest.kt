@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlinx.cinterop.*
import asan.*

@OptIn(ExperimentalForeignApi::class)
actual fun runAsanTest() {
    val size = 10
    val overflow = 1000
    if (size > overflow) {
        println("size wins")
    } else {
        println("overflow wins")
    }
}
