@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.example.app

import kotlin.native.CName

// ═══════════════════════════════════════════════════════════════════
// Scenario 2: @CName — controlling the C-visible symbol name
// ═══════════════════════════════════════════════════════════════════

// ── 2A: externName only ──
// Creates a standalone `extern "C"` function callable directly from C
// without going through the ExportedSymbols struct.
// C signature: extern KInt kn_subtract(KInt, KInt);
@CName("kn_subtract")
fun subtractNumbers(a: Int, b: Int): Int = a - b

// ── 2B: shortName only ──
// Changes the member name inside the ExportedSymbols struct.
// No standalone extern "C" function is generated.
// Access: symbols()->kotlin.root.com.example.app.myMultiply(...)
@CName(externName = "", shortName = "myMultiply")
fun multiplyNumbers(a: Int, b: Int): Int = a * b

// ── 2C: both externName and shortName ──
// Creates BOTH a standalone extern "C" function AND a custom struct member.
// C direct call: kn_divide(a, b)
// C struct access: symbols()->kotlin.root.com.example.app.div(...)
@CName(externName = "kn_divide", shortName = "div")
fun divideNumbers(a: Double, b: Double): Double = if (b != 0.0) a / b else Double.NaN

// ── 2D: No @CName (for comparison) ──
// Default auto-generated name in struct: "defaultNamedFunction"
fun defaultNamedFunction(): String = "I use the default name"

// ── 2E: externName with string-returning function ──
// Demonstrates that String return maps to const char* in C.
@CName("kn_hello")
fun helloFromKotlin(): String = "Hello from Kotlin/Native!"
