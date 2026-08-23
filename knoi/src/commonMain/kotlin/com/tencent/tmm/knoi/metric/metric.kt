package com.tencent.tmm.knoi.metric

expect fun <T> trace(name: String, block: () -> T): T

fun trace(name: String) {
    trace(name) {}
}