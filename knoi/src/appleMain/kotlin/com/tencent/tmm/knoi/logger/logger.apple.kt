package com.tencent.tmm.knoi.logger

import platform.Foundation.NSLog
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual inline fun debug(message: String) {
    if (Platform.isDebugBinary) {
        NSLog(message)
    }
}

actual fun info(message: String) {
    NSLog(message)
}

actual fun warming(message: String) {
    NSLog(message)
}

actual fun e(message: String) {
    NSLog(message)
}