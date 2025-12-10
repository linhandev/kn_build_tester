package com.example

import com.example.caller.CallerApi
import com.example.src.getSrcMessage
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName

@OptIn(ExperimentalNativeApi::class)
@CName("run_demo")
fun runDemo() {
    println("=== Kotlin Native Partial Linkage Demo ===")
    println()
    
    // Test Src Lib
    println("Test Src: ${getSrcMessage()}")
    println()
    
    // Create API instance
    val api = CallerApi()
    
    // Test 1: Welcome user (uses greetUser from dep-lib)
    println("Test 1: Welcome User")
    val greeting = api.welcomeUser("Alice")
    println("  Result: $greeting")
    println()
    
    // Test 2: Add numbers (uses calculateSum from dep-lib)
    println("Test 2: Add Numbers")
    val sum = api.addNumbers(10, 20)
    println("  Result: 10 + 20 = $sum")
    println()
    
    // Test 3: Format user (uses UserData and processUserData from dep-lib)
    println("Test 3: Format User")
    val userInfo = api.formatUser("Bob", 25)
    println("  Result: $userInfo")
    println()
    
    // Test 4: Get library info (uses ConfigHelper from dep-lib)
    println("Test 4: Library Info")
    val libInfo = api.getLibraryInfo()
    println("  Result: $libInfo")
    println()
    
    println()
    println("=== All tests completed successfully! ===")
}
