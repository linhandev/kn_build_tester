@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

@CName("kn_helloworld_ios")
fun helloworld(): String = "Hello from Kotlin/Native on iOS"

@CName("kn_add_ios")
fun add(a: Int, b: Int): Int = a + b
