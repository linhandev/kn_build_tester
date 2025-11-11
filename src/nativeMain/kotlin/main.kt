@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import kotlinx.cinterop.*

@ExperimentalForeignApi
@CName("subtract_numbers")
fun subtractNumbers(a: Int, b: Int): Int {
    return znowTest.implementedFunction()
}

// // Exported main to satisfy HarmonyOS/OHOS linker requirements
// // This won't be called when loaded as a shared library
// @ExperimentalForeignApi
// @CName("main")
// fun konanMain(): Int {
//     return 0
// }