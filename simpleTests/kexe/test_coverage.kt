fun processNumber(n: Int): String {
    return if (n > 10) {
        "Number is greater than 10"  // This branch WILL execute
    } else {
        "Number is 10 or less"       // This branch will NOT execute
    }
}

fun calculateSum(a: Int, b: Int): Int {
    val sum = a + b
    println("Sum of $a and $b is $sum")
    return sum
}

fun main(args: Array<String>) {
    println("=== GCOV Coverage Test ===")
    
    // Call with value > 10 to execute only one branch
    val result = processNumber(20)
    println("Result: $result")
    
    // This function will be fully covered
    val sum = calculateSum(5, 7)
    
    println("Test completed successfully with sum=$sum")
}
