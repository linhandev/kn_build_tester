package com.example.dep

/**
 * Initial version of the dependency library.
 * This represents the "stable" API that caller-lib depends on.
 */

class UserData(val name: String, val age: Int)

fun greetUser(name: String): String {
    return "Hello, $name!"
}

fun calculateSum(a: Int, b: Int): Int {
    return a + b
}

fun processUserData(data: UserData): String {
    return "User: ${data.name}, Age: ${data.age}"
}

object ConfigHelper {
    const val VERSION = "1.0.0"
    
    fun getConfigValue(): String {
        return "config-value"
    }
}
