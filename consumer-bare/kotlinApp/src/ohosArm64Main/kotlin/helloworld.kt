@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import platform.PerformanceAnalysisKit.HiLog.OH_LOG_Print
import platform.PerformanceAnalysisKit.HiLog.LOG_APP
import platform.PerformanceAnalysisKit.HiLog.LOG_INFO
// a->b test: AssetApi (depends AssetType) — both from the cpf 0.4-built hilog-klib (maven local).
import platform.AssetStoreKit.AssetApi.OH_Asset_FreeBlob
import platform.AssetStoreKit.AssetType.Asset_Blob

@CName("kn_helloworld")
fun helloworld(): String {
    OH_LOG_Print(LOG_APP, LOG_INFO, 0x1234u, "kn_demo", "HiLog klib OK; testing AssetApi->AssetType")
    // Exercise a symbol from AssetApi that references a type (Asset_Blob) defined in AssetType —
    // proves the a->b klib dependency resolves at compile + link time. Pass null (free of nothing).
    OH_Asset_FreeBlob(null)
    return "Hello from Kotlin/Native"
}
