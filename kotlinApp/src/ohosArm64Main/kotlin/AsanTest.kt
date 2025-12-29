@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlinx.cinterop.*
import asan.*

@OptIn(ExperimentalForeignApi::class)
actual fun runAsanTest() {
    val size = 10
    val overflow = 1000
    
    // Create ByteArray on Kotlin heap (not native heap)
    val buffer = ByteArray(size) { 'A'.code.toByte() }
    println("Kotlin: Created ByteArray on Kotlin heap, size: $size")
    
    // Create another object nearby on Kotlin heap (potential canary/victim)
    val canary = ByteArray(size) { 'C'.code.toByte() }
    println("Canary before overflow: ${canary.decodeToString()}")
    
    println("Kotlin: Calling C function to overflow by $overflow bytes")
    canary.usePinned { pinned ->
        println("pinned address ${pinned.addressOf(0)}")
    }

    // Pin ByteArray to get pointer to actual Kotlin heap memory (not a copy)
    buffer.usePinned { pinned ->
        println("pinned address ${pinned.addressOf(0)}")
        trigger_overflow(pinned.addressOf(0), size, overflow)
    }

    println("Kotlin: Buffer after overflow: ${buffer.decodeToString()}")
    println("Canary after overflow: ${canary.decodeToString()}")
}
