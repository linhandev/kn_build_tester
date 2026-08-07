@file:OptIn(
    kotlin.experimental.ExperimentalNativeApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
)

package com.example.k2n

import com.example.k2n.cb.k2n_invoke_cb
import com.example.k2n.cb.k2n_register_cb
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.staticCFunction
import platform.PerformanceAnalysisKit.HiLog.LOG_APP
import platform.PerformanceAnalysisKit.HiLog.LOG_INFO
import platform.PerformanceAnalysisKit.HiLog.OH_LOG_PrintMsg
import platform.posix.strdup

private const val DOMAIN = 0x0A01u

private var n2kHit: Int = 0

/** C→K (n2k): registered via staticCFunction, invoked from C. */
private fun onK2nCallback(x: Int) {
    n2kHit = x * 2
    OH_LOG_PrintMsg(LOG_APP, LOG_INFO, DOMAIN, "k2n-n2k", "callback x=$x hit=$n2kHit")
}

/**
 * Entry for on-demand load of libk2n.so.
 * Contains both K→C (HiLog / k2n) and C→K (staticCFunction / n2k).
 */
@CName("kn_k2n_run")
fun knK2nRun(): CPointer<ByteVar>? {
    // k2n: Kotlin → C (platform HiLog)
    OH_LOG_PrintMsg(LOG_APP, LOG_INFO, DOMAIN, "k2n", "k2n module: HiLog call")

    // n2k: register Kotlin callback, then C invokes it (CMP-like staticCFunction)
    n2kHit = 0
    k2n_register_cb(staticCFunction(::onK2nCallback))
    val invoked = k2n_invoke_cb(21)
    val msg = "k2n.so: k2n=ok n2k=invoked:$invoked hit=$n2kHit"
    OH_LOG_PrintMsg(LOG_APP, LOG_INFO, DOMAIN, "k2n", msg)
    return strdup(msg)
}
