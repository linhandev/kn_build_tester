@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.example.app

import kotlin.native.internal.ExportedBridge

// ═══════════════════════════════════════════════════════════════════
// Scenario 7: @ExportedBridge — raw C-callable symbols (no header)
// ═══════════════════════════════════════════════════════════════════
//
// @ExportedBridge creates a C-callable function with the EXACT name given,
// as a raw LLVM symbol. Key differences from @CName:
//
//   - NO entry in the generated .h header file
//   - The C caller must manually declare the function prototype
//   - The symbol name is used verbatim (no prefix, no mangling)
//   - Prevents dead-code elimination
//   - Generates C-to-Kotlin bridge with thread-state switching
//
// This is primarily used internally by Swift Export, but is available
// as an experimental API for advanced use cases.

@ExportedBridge("kn_raw_add")
fun rawAdd(a: Int, b: Int): Int = a + b

@ExportedBridge("kn_raw_greeting")
fun rawGreeting(): String = "Hello via raw bridge!"

@ExportedBridge("kn_raw_strlen")
fun rawStringLength(str: String): Int = str.length
