@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.example.cexport.klib

import kotlin.native.CName

// Scenario 3: Exports from a klib dependency
// These functions are in a module that will be compiled as a klib
// When the app module depends on this klib, will these functions be exported?

@CName("cexport_klib_version")
fun klibVersion(): String = "1.0.0 (from klib module)"

@CName("cexport_klib_concat")
fun klibConcat(a: String, b: String): String = "$a-$b"
