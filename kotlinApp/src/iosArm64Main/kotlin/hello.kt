@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

@CName("kn_helloworld_ios")
fun helloworld(): String = "Hello from Kotlin/Native on iOS"

@CName("kn_add_ios")
fun add(a: Int, b: Int): Int = a + b

@CName("kn_greet_ios")
fun greet(name: String): String = "Hello, $name! Welcome to KMP on iOS."

// A slightly more complex function to generate more bitcode
@CName("kn_fibonacci_ios")
fun fibonacci(n: Int): Long {
    if (n <= 1) return n.toLong()
    var a = 0L
    var b = 1L
    for (i in 2..n) {
        val temp = a + b
        a = b
        b = temp
    }
    return b
}
