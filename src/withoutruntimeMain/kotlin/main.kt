@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

@CName("wo_subtract_numbers")
fun subtractNumbers(a: Int, b: Int): Int {
    return a - b
}

@CName("wo_divide_numbers")
fun divideNumbers(a: Int, b: Int): Int {
    // Division implementation without circular dependency
    // In a real circular dependency scenario, this would try to call
    // with_multiply_numbers from the withruntime klib, but that's not possible
    return if (b != 0) a / b else 0
}

@CName("wo_modulo_numbers")
fun moduloNumbers(a: Int, b: Int): Int {
    // Additional math operation - modulo function
    return if (b != 0) a % b else 0
}
