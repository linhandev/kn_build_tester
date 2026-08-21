@file:OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.strdup

// CExport V1 + @CName: CAdapter writes initRuntime + Runnable + String↔char*.
@CName("kn_cname_add")
fun cnameAdd(a: Int, b: Int): Int = a + b

@CName("kn_cname_alloc")
fun cnameAlloc(): Int = "hello".length

@CName("kn_cname_greet")
fun cnameGreet(name: String): String = "hi $name"

@CName("kn_cname_caught")
fun cnameCaught(): Int =
    try {
        throw RuntimeException("x")
    } catch (_: Throwable) {
        7
    }

// @ExportedBridge: N2K stub already does initRuntime + thread state.
// Fill-in = CAdapter's char*↔String. internal so V1 does not also wrap it.
@ExportedBridge("kn_eb_add")
internal fun ebAdd(a: Int, b: Int): Int = a + b

@ExportedBridge("kn_eb_alloc")
internal fun ebAlloc(): Int = "hello".length

@ExportedBridge("kn_eb_greet")
internal fun ebGreet(name: CPointer<ByteVar>?): CPointer<ByteVar>? {
    val s = "hi ${name?.toKString() ?: ""}"
    return strdup(s)
}

@ExportedBridge("kn_eb_caught")
internal fun ebCaught(): Int =
    try {
        throw RuntimeException("x")
    } catch (_: Throwable) {
        7
    }
