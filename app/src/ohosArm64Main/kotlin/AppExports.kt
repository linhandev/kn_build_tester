@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.example.cexport.app

import kotlin.native.CName
import com.example.cexport.subproject.subprojectGreet
import com.example.cexport.subproject.subprojectMultiply
import com.example.cexport.klib.klibVersion
import com.example.cexport.klib.klibConcat

// Scenario 1: Top-level exports from the main module
// These functions are directly in the module that produces the binary

@CName("cexport_app_hello")
fun appHello(): String = "Hello from app module (Scenario 1: top-level)"

@CName("cexport_app_add")
fun appAdd(a: Int, b: Int): Int = a + b

// Bridge functions that call into dependency modules
// This ensures the dependency code is not dead-code eliminated
@CName("cexport_bridge_greet")
fun bridgeGreet(name: String): String = subprojectGreet(name)

@CName("cexport_bridge_multiply")
fun bridgeMultiply(a: Int, b: Int): Int = subprojectMultiply(a, b)

@CName("cexport_bridge_version")
fun bridgeVersion(): String = klibVersion()

@CName("cexport_bridge_concat")
fun bridgeConcat(a: String, b: String): String = klibConcat(a, b)
