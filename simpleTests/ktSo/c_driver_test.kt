@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package libdemo

@CName("compute_factorial")
fun computeFactorial(n: Int): Int {
    return if (n <= 1) {
        1
    } else {
        n * computeFactorial(n - 1)
    }
}

@CName("is_prime")
fun isPrime(n: Int): Boolean {
    if (n < 2) return false
    for (i in 2 until n) {
        if (n % i == 0) return false
    }
    return true
}

@CName("library_init")
fun initLibrary() {
    println("[Kotlin] Library initialized")
}

// Dummy main function for shared library - never called when loaded via dlopen
fun main(args: Array<String>) {
    // This main is required by Kotlin Native runtime but won't be executed
    // when the library is loaded via dlopen()
}
