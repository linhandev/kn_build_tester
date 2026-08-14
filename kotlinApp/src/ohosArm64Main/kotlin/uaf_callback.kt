@file:OptIn(
    kotlin.experimental.ExperimentalNativeApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
)

import kotlinx.cinterop.*
import mallocHeap_kotlinCallback_useAfterFree.*

@CName("kn_run_mallocHeap_kotlinCallback_useAfterFree")
fun run_mallocHeap_kotlinCallback_useAfterFree() {
    val p = c_malloc_n(8)!!
    free_then_cb(p, staticCFunction { q ->
        q!![0] = 1
    })
}

fun main() {
    run_mallocHeap_kotlinCallback_useAfterFree()
}
