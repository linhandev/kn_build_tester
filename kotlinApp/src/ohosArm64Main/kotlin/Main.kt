@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

@CName("kn_add")
fun add(a: Int, b: Int): Int = a + b

@CName("kn_greet")
fun greet(): String = "Hello from Kotlin/Native on OHOS arm64"
