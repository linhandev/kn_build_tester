package com.tencent.tmm.knoi.metric

import com.tencent.tmm.knoi.logger.info
import com.tencent.tmm.knoi.logger.isDebug
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValues
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.reinterpret
import platform.posix.dlopen
import platform.posix.dlsym

const val RTLD_NOW = 2
var beginTrace: CPointer<CFunction<(CValues<ByteVar>) -> Unit>>? = null
var endTrace: CPointer<CFunction<() -> Unit>>? = null

fun initTraceFuncIfNeed() {
    if (isDebug) {
        // 采用 dlsym 动态查找符号，因为 Kotlin Native 使用的 鸿蒙 SDK 版本为 sdk 9，无法正常链接。
        val handle = dlopen("libhitrace_ndk.z.so", RTLD_NOW)
        beginTrace = dlsym(handle, "OH_HiTrace_StartTrace")?.reinterpret()
        endTrace = dlsym(handle, "OH_HiTrace_FinishTrace")?.reinterpret()
        info("initTraceFunc beginTrace != null ${beginTrace != null}  endTrace != null ${endTrace != null}")
    }
}

actual fun <T> trace(name: String, block: () -> T): T {
    if (isDebug) {
        return try {
            beginTrace?.invoke("knoi:$name".cstr)
            block()
        } finally {
            endTrace?.invoke()
        }
    } else {
        return block()
    }
}