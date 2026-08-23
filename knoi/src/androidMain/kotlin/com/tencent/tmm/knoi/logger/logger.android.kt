package com.tencent.tmm.knoi.logger

import android.util.Log

actual fun info(message: String) {
    Log.i(tag,message)
}

actual fun debug(message: String) {
    if (isDebug){
        Log.d(tag,message)
    }
}

actual fun warming(message: String) {
    Log.w(tag,message)
}

actual fun e(message: String) {
    Log.e(tag,message)
}