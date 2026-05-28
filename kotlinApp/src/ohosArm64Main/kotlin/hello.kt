@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

@CName("kn_helloworld")
fun helloworld(): String = "Hello from Kotlin/Native on OHOS"

@CName("kn_add")
fun add(a: Int, b: Int): Int = a + b
