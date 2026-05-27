@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.example.cexport.subproject

import kotlin.native.CName

// Scenario 2: Exports from a dependency subproject (submodule)
// These functions are in a separate Gradle module that is depended on by the main app module
// Question: Will these appear in the final binary's exported header?

@CName("cexport_subproject_greet")
fun subprojectGreet(name: String): String = "Hello, $name! (from subproject module)"

@CName("cexport_subproject_multiply")
fun subprojectMultiply(a: Int, b: Int): Int = a * b
