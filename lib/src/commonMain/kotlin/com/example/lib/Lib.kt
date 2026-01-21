package com.example.lib

/**
 * Library - Base library that will be published and used by app.
 */

data class User(val name: String, val age: Int)

fun greetUser(user: User): String {
    return "Hello, ${user.name}! You are ${user.age} years old."
}

object LibConfig {
    const val VERSION = "1.0.0"
    
    fun getInfo(): String = "lib v$VERSION"
}

object Calculator {
    fun add(a: Int, b: Int): Int = a + b
    fun multiply(a: Int, b: Int): Int = a * b
}
