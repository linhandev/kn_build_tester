@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.example.app

// ═══════════════════════════════════════════════════════════════════
// Scenario 1: Top-level functions with DEFAULT naming
// ═══════════════════════════════════════════════════════════════════
// These functions appear in the ExportedSymbols struct under:
//   symbols()->kotlin.root.com.example.app.<functionName>
// No @CName annotation — the compiler generates names from the Kotlin FQN.

fun addNumbers(a: Int, b: Int): Int = a + b

fun greetUser(name: String): String = "Hello, $name!"

fun concatenate(a: String, b: String): String = "$a $b"

val appVersion: String = "1.0.0"

var requestCount: Int = 0
