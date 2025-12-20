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
        buffer[i] = i.toByte()
    }
    
    println("Kotlin: Calling C function to overflow by $overflow bytes")
    trigger_overflow(buffer, size, overflow)
    
    println("Kotlin: Freeing memory")
    nativeHeap.free(buffer)
}
