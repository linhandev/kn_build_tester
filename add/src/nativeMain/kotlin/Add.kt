@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package add

import add.addcfun

// Minimal Kotlin usage so the cinterop klib is built and published.
// Root project will depend on this klib and export add_numbers to C.
fun addNumbersInternal(a: Int, b: Int): Int = addcfun(a, b)
