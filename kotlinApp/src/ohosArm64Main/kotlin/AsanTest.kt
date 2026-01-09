@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlinx.cinterop.*
import asan.*

@OptIn(ExperimentalForeignApi::class)
actual fun runAsanTest() {
    val a = 1 / 0
    fun recurse(level: Int) {
        if (level < 64) {
            recurse(level + 1)
        } else {
            // Stay at level 64 and create throwables in a loop
            val startTime = kotlin.system.getTimeNanos()
            for (i in 0..3000) {
                val throwable = Throwable("Test exception at iteration $i")
                val stackTraceString = throwable.stackTraceToString()
                // Process stacktrace (e.g., print or analyze)
                println("Iteration $i: Stack trace:\n$stackTraceString")
            }
            val endTime = kotlin.system.getTimeNanos()
            val durationNanos = endTime - startTime
            val durationMillis = durationNanos / 1_000_000.0
            val durationSeconds = durationNanos / 1_000_000_000.0
            println("For loop timing: ${durationNanos} ns (${durationMillis} ms, ${durationSeconds} s)")
        }
    }
    recurse(0)
}
