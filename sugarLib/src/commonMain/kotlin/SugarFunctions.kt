package com.example.sugarlib

// Extension function - tricky for coverage
fun String.processWithExtension(): String {
    return this.uppercase()
        .reversed()
        .takeIf { it.length > 3 } ?: "short"
}

// Infix function with subject-based when - tricky for coverage
infix fun Int.myPowerOf(exponent: Int): Int {
    return when (exponent) {
        0 -> 1
        -1 -> 0
        else -> {
            var result = 1
            repeat(exponent) {
                result *= this
            }
            result
        }
    }
}

// Lambda with receiver - tricky for coverage
fun processWithLambda(input: Int): String {
    return buildString {
        append("Value: ")
        append(input)
        if (input > 10) {
            append(" (large)")
        } else {
            append(" (small)")
        }
    }
}

// When expression with sealed class - tricky for coverage
sealed class Result {
    data class Success(val value: Int) : Result()
    data class Error(val message: String) : Result()
    object Loading : Result()
}

fun processResult(result: Result): String {
    return when (result) {
        is Result.Success -> "Success: ${result.value}"
        is Result.Error -> "Error: ${result.message}"
        Result.Loading -> "Loading..."
    }
}

// Inline function with reified type parameter and condition - tricky for coverage
inline fun <reified T> processType(value: Any): String {
    return when (value) {
        is T -> "Is ${T::class.simpleName}"
        else -> "Not ${T::class.simpleName}"
    }
}

// Main caller function that calls the callee with syntactic sugar
// Intentionally only runs some branches to test coverage reporting
fun complexSugarFunction(): String {
    val typeMatch = processType<Int>(42)

    // Only call extension function with long string (skip short path)
    val longStr = "hello".processWithExtension()
    
    // Only call infix function with positive exponent (skip 0 and -1 branches)
    val powerPositive = 2 myPowerOf 3
    
    // Only call lambda with small value (skip large branch)
    val smallLambda = processWithLambda(5)
    
    // Only call when with sealed class - Success branch only (skip Error and Loading)
    val successResult = processResult(Result.Success(42))
    
    return buildString {
        appendLine("Extension: $longStr")
        appendLine("Power: $powerPositive")
        appendLine("Lambda: $smallLambda")
        appendLine("Result: $successResult")
        appendLine("Type: $typeMatch")
    }
}
