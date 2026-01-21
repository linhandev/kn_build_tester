package com.example.app

import com.example.lib.User
import com.example.lib.greetUser
import com.example.lib.LibConfig
import com.example.lib.Calculator
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName

/**
 * App - Depends on lib (via published metadata klib).
 */

fun runDemo(): String {
    val results = StringBuilder()
    
    val user = User("Alice", 30)
    results.appendLine("Created user: $user")
    
    val greeting = greetUser(user)
    results.appendLine("Greeting: $greeting")
    
    val libInfo = LibConfig.getInfo()
    results.appendLine("Lib info: $libInfo")
    
    val sum = Calculator.add(10, 20)
    val product = Calculator.multiply(5, 6)
    results.appendLine("Calculator: 10 + 20 = $sum, 5 * 6 = $product")
    
    return results.toString()
}

@OptIn(ExperimentalNativeApi::class)
@CName("run_app_demo")
fun runAppDemo(): Unit {
    println("=== Metadata Klib Demo ===")
    println(runDemo())
    println("=== Demo Complete ===")
}
