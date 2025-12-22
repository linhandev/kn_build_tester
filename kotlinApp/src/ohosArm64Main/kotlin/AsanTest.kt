@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlinx.cinterop.*
import asan.*

@OptIn(ExperimentalForeignApi::class)
actual fun runAsanTest() {
    val size = 10
    val overflow = 1000
    println("Kotlin: Allocating $size bytes on native heap")
    val buffer = nativeHeap.allocArray<ByteVar>(size)
    
    // Initialize
    for (i in 0 until size) {
        buffer[i] = 0.toByte()
    }
    
    // Create another string in the heap
    val maybeCanary = nativeHeap.allocArray<ByteVar>(size)
    for (i in 0 until size) {
        maybeCanary[i] = 0.toByte()
    }
    println("Canary string before triggering overflow: ${maybeCanary.toKString()}")

    println("Kotlin: Calling C function to overflow by $overflow bytes")
    trigger_overflow(buffer, size, overflow)
    
    println("Canary string after triggering overflow: ${maybeCanary.toKString()}")

    println("Kotlin: Freeing memory")
    nativeHeap.free(buffer)
    nativeHeap.free(maybeCanary)
}
