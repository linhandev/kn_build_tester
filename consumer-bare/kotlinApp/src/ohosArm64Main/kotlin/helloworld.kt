@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import platform.PerformanceAnalysisKit.HiLog.OH_LOG_Print
import platform.PerformanceAnalysisKit.HiLog.LOG_APP
import platform.PerformanceAnalysisKit.HiLog.LOG_INFO
// a->b test: AssetApi (depends AssetType) — both from the cpf 0.4-built ohos-capi (maven local).
import platform.AssetStoreKit.AssetApi.OH_Asset_FreeBlob
import platform.AssetStoreKit.AssetType.Asset_Blob
// staticLibraries demo: mylib_add/mylib_answer come from .a embedded in static-lib-demo klib.
import demo.mylib.mylib_add
import demo.mylib.mylib_answer
// posix demo: getenv comes from the posix klib (com.example:ohos-capi-cinterop-posix), built from
// the dist-only posix.def now checked into the producer. Tests that a Linux-base platform lib
// resolves at compile + link + runtime on ohos (carries linkerOpts -lresolv -lm -lpthread ...).
import platform.posix.getenv
import kotlinx.cinterop.toKString

@CName("kn_helloworld")
fun helloworld(): String {
    OH_LOG_Print(LOG_APP, LOG_INFO, 0x1234u, "kn_demo", "HiLog klib OK; testing AssetApi->AssetType")
    // Exercise a symbol from AssetApi that references a type (Asset_Blob) defined in AssetType —
    // proves the a->b klib dependency resolves at compile + link time. Pass null (free of nothing).
    OH_Asset_FreeBlob(null)
    // staticLibraries demo: .a linked automatically by KGP (no -L/-l), symbol resolves at link.
    val r = mylib_add(mylib_answer(), mylib_answer())  // 42 + 42 = 84
    OH_LOG_Print(LOG_APP, LOG_INFO, 0x1234u, "kn_demo", "static-lib-demo mylib_add(42,42)=$r")
    // posix demo: read PATH env var. Proves the posix klib (Linux-base platform lib) resolves end
    // to end — getenv symbol from our com.example posix klib, libc at runtime on the device.
    val path = getenv("PATH")?.toKString() ?: "<null>"
    OH_LOG_Print(LOG_APP, LOG_INFO, 0x1234u, "kn_demo", "posix getenv(PATH)=$path")
    return "Hello from Kotlin/Native"
}
