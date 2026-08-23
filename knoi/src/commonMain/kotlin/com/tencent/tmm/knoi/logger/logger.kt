package com.tencent.tmm.knoi.logger

import kotlin.concurrent.Volatile

const val tag = "knoi"

@Volatile
var isDebug: Boolean = true

expect fun info(message: String)

expect fun debug(message: String)

expect fun warming(message: String)

expect fun e(message: String)

fun check(message: String, condition: () -> Boolean) {
    if (condition.invoke()) {
        error(message)
    }
}