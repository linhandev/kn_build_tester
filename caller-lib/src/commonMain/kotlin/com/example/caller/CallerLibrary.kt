package com.example.caller

import com.example.dep.*

/**
 * Caller library that uses the dep-lib API.
 * This library will be compiled against the initial version of dep-lib.
 */

class CallerApi {
    fun welcomeUser(name: String): String {
        // Uses greetUser from dep-lib
        return greetUser(name)
    }
    
    fun addNumbers(x: Int, y: Int): Int {
        // Uses calculateSum from dep-lib
        return calculateSum(x, y)
    }
    
    fun formatUser(name: String, age: Int): String {
        // Uses UserData and processUserData from dep-lib
        val userData = UserData(name, age)
        return processUserData(userData)
    }
    
    fun getLibraryInfo(): String {
        // Uses ConfigHelper from dep-lib
        return "Caller using dep-lib ${ConfigHelper.VERSION}: ${ConfigHelper.getConfigValue()}"
    }
}

class NonAbstract : Abstract() {
    override fun method1() {
        println("NonAbstract method1 implementation")
    }
}

class Implementation : Interface {
    override fun method1() {
        println("Implementation method1 implementation")
    }
}