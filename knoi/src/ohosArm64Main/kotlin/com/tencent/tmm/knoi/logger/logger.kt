package com.tencent.tmm.knoi.logger

import platform.ohos.LOG_APP
import platform.ohos.LOG_DEBUG
import platform.ohos.LOG_ERROR
import platform.ohos.LOG_INFO
import platform.ohos.LOG_WARN
import platform.ohos.LogLevel
import platform.ohos.OH_LOG_Print
import kotlin.concurrent.Volatile

const val domain = 1u


@Volatile
var logProxy: (LogLevel, String) -> Unit = { level: LogLevel, message: String ->
    OH_LOG_Print(LOG_APP, level, domain, tag, message)
}

actual fun info(message: String) {
    logProxy(LOG_INFO, message)
}

actual inline fun debug(message: String) {
    if (Platform.isDebugBinary && isDebug) {
        logProxy(LOG_DEBUG, message)
    }
}

actual fun warming(message: String) {
    logProxy(LOG_WARN, message)
}

actual fun e(message: String) {
    logProxy(LOG_ERROR, message)
}
