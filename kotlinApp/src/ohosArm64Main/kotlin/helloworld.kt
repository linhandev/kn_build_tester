@file:OptIn(ExperimentalNativeApi::class)

import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.internal.ExportedBridge

// No-arg C entries (ArkTS register 同类). CName: CAdapter 壳做 initRuntime/线程态。
// EB stackmap ON: N2K stub 做同样的事。EB stackmap OFF: 函数自己 needsRuntimeInit+switchToRunnable。
@CName("kn_cname_ping")
fun cnamePing() {
    Unit
}

@CName("kn_cname_alloc")
fun cnameAlloc() {
    "hello".length
}

@CName("kn_cname_throw")
fun cnameThrow() {
    throw RuntimeException("cname")
}

@ExportedBridge("kn_eb_ping")
internal fun ebPing() {
    Unit
}

@ExportedBridge("kn_eb_alloc")
internal fun ebAlloc() {
    "hello".length
}

@ExportedBridge("kn_eb_throw")
internal fun ebThrow() {
    throw RuntimeException("eb")
}
