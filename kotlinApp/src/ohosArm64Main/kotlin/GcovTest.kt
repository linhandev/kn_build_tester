@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import gcov.*
import com.example.switchlib.processValue
import com.example.switchlib.processValueCond
import com.example.sugarlib.complexSugarFunction

@kotlinx.cinterop.ExperimentalForeignApi
actual fun runGcovTest() {
    try {
        platform.PerformanceAnalysisKit.Hitrace.OH_HiTrace_IsTraceEnabled()
    } catch (e: Throwable) {
        println("asdfasdf" + e.message)
        e.printStackTrace()
    }

    val size = 10
    val overflow = 1000
    if (size > overflow) {
        println("size wins")
    } else {
        println("overflow wins")
    }
    gcov.test_gcov_c()

    // Test the switchLib dependency
    processValue()
    processValueCond()
    
    // Test the sugarLib dependency with complex syntactic sugar
    complexSugarFunction()
}
