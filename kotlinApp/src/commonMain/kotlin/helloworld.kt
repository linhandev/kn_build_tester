@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

@CName("kn_helloworld")
fun helloworld(): String = "Hello from Kotlin/Native on ${getPlatform()}"

expect fun getPlatform(): String
