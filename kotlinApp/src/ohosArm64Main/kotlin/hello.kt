@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

@CName("kn_helloworld")
fun helloworld(): String = "Hello from Kotlin/Native on OHOS"

@CName("kn_add")
fun add(a: Int, b: Int): Int = a + b

@CName("kn_greet")
fun greet(name: String): String = "Hello, $name! Welcome to KMP."

// A slightly more complex function to generate more bitcode
@CName("kn_fibonacci")
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
