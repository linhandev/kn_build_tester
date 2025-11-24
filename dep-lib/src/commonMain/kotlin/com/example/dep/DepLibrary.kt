package com.example.dep

/**
 * Initial version of the dependency library.
 * caller-lib is compatible with this version of dep and wont be updated.
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
