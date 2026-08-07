@file:OptIn(
    kotlin.experimental.ExperimentalNativeApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
)

package com.example.n2k

import com.example.n2k.cb.n2k_invoke_cb
import com.example.n2k.cb.n2k_register_cb
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.staticCFunction
import platform.PerformanceAnalysisKit.HiLog.LOG_APP
import platform.PerformanceAnalysisKit.HiLog.LOG_INFO
import platform.PerformanceAnalysisKit.HiLog.OH_LOG_PrintMsg
import platform.posix.strdup

private const val DOMAIN = 0x0A02u

private var n2kHit: Int = 0

/** C→K (n2k): registered via staticCFunction, invoked from C. */
private fun onN2kCallback(x: Int) {
    n2kHit = x * 3
    OH_LOG_PrintMsg(LOG_APP, LOG_INFO, DOMAIN, "n2k-cb", "callback x=$x hit=$n2kHit")
}

/**
 * Entry for on-demand load of libn2k.so.
 * Independent of :k2n — also exercises both K→C and C→K.
 */
@CName("kn_n2k_run")
fun knN2kRun(): CPointer<ByteVar>? {
    OH_LOG_PrintMsg(LOG_APP, LOG_INFO, DOMAIN, "n2k", "n2k module: HiLog call")

    n2kHit = 0
    n2k_register_cb(staticCFunction(::onN2kCallback))
    val invoked = n2k_invoke_cb(7)
    val msg = "n2k.so: k2n=ok n2k=invoked:$invoked hit=$n2kHit"
    OH_LOG_PrintMsg(LOG_APP, LOG_INFO, DOMAIN, "n2k", msg)
    return strdup(msg)
}
