@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

@CName("with_add_numbers")
fun addNumbers(a: Int, b: Int): Int {
    return a + b
}

@CName("with_multiply_numbers")
fun multiplyNumbers(a: Int, b: Int): Int {
    // Multiplication implementation without circular dependency
    // In a real circular dependency scenario, this would try to call
    // wo_divide_numbers from the withoutruntime klib, but that's not possible
    return a * b
}

@CName("with_power_numbers")
fun powerNumbers(base: Int, exponent: Int): Int {
    // Additional math operation - power function
    var result = 1
    repeat(exponent) {
        result *= base
    }
    return result
}
