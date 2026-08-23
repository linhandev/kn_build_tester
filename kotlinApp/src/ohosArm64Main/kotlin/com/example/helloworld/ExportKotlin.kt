package com.example.helloworld

import org.cpf.kotlin.akinterop.annotation.KNExportClass
import org.cpf.kotlin.akinterop.annotation.KNExportFunction

@KNExportFunction
fun greet(name: String): String = "Hello, $name!"

@KNExportClass(exportAll = true)
class Counter {
    private var count: Int = 0
    fun increment(): Int { count++; return count }
    fun getCount(): Int = count
    fun reset() { count = 0 }
}
