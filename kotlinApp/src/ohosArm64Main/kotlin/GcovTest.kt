@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlinx.cinterop.*
import gcov.*

@OptIn(ExperimentalForeignApi::class)
actual fun runGcovTest() {
    val size = 10
    val overflow = 1000
    if (size > overflow) {
        println("size wins")
    } else {
        println("overflow wins")
    }
    gcov.test_gcov_c()
}
