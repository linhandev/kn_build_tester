import kotlin.time.measureTime
import kotlin.time.DurationUnit
import hello.add_numbers
import hello.c_empty_function
import kotlinx.cinterop.*

fun ktEmptyFunction() {}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun perfFunctionCall() {
    val iterations = listOf(100, 10_000, 10_000_000, 1000_000_000)
    
    println("Performance comparison: Kotlin vs C-style empty function calls")
    println("===============================================================")
    
    for (count in iterations) {
        println("\nTesting with $count calls:")
        
        // Test Kotlin function calls
        val kotlinTime = measureTime {
            repeat(count) {
                ktEmptyFunction()
            }
        }
        
        // Test C-style function calls (simulated for now)
        val cTime = measureTime {
            repeat(count) {
                c_empty_function()
            }
        }
        
        val kotlinMs = kotlinTime.toDouble(DurationUnit.MILLISECONDS)
        val cMs = cTime.toDouble(DurationUnit.MILLISECONDS)
        
        println("  Kotlin function: ${kotlinMs.toString().take(6)}ms")
        println("  C-style function: ${cMs.toString().take(6)}ms")
        
        if (kotlinMs > 0 && cMs > 0) {
            val ratio = kotlinMs / cMs
            val ratioStr = ratio.toString().take(4)
            println("  Ratio (Kotlin/C): ${ratioStr}x")
        } else {
            println("  Ratio: Too fast to measure accurately")
        }
        
        if (count >= 10_000) {
            val kotlinNsPerCall = kotlinTime.toDouble(DurationUnit.NANOSECONDS) / count
            val cNsPerCall = cTime.toDouble(DurationUnit.NANOSECONDS) / count
            val kotlinNsStr = kotlinNsPerCall.toString().take(6)
            val cNsStr = cNsPerCall.toString().take(6)
            println("  Kotlin: ${kotlinNsStr} ns/call")
            println("  C-style: ${cNsStr} ns/call")
        }
    }
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun main() {
    println("1 + 2 = ${add_numbers(1, 2)}")
    
    perfFunctionCall()
}

