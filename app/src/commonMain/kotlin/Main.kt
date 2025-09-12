import mathlib.mathLibAddFunction
import mathlib.printPlatformInfo
import stringlib.stringLibFunction
import stringlib.printStringPlatformInfo

fun main() {
    println("=== Kotlin Multi-Module Demo ===")
    
    // Show platform information
    printPlatformInfo()
    printStringPlatformInfo()
    
    // Test static library function
    val mathResult = mathLibAddFunction(5, 3)
    println("Result from static library: $mathResult")
    
    // Test dynamic library function
    val stringResult = stringLibFunction("Hello from app!")
    println("Result from dynamic library: $stringResult")
    
    println("=== Demo completed ===")
}
