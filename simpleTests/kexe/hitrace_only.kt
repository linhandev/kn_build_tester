@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

@kotlinx.cinterop.ExperimentalForeignApi
fun main() {
    try {
        platform.PerformanceAnalysisKit.Hitrace.OH_HiTrace_IsTraceEnabled()
    } catch (e: Throwable) {
        println("asdfasdf" + e.message)
        e.printStackTrace()
    }
}
