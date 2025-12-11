package com.example

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName
import com.example.src.triggerErrors

@OptIn(ExperimentalNativeApi::class)
@CName("run_demo")
fun runDemo() {
    triggerErrors()
}

fun main() {
    runDemo()
}
