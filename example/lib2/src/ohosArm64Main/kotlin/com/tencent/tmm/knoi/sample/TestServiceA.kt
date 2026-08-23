package com.tencent.tmm.knoi.sample

import com.tencent.tmm.knoi.annotation.ServiceConsumer
import com.tencent.tmm.knoi.type.ArrayBuffer
import com.tencent.tmm.knoi.type.JSValue

@ServiceConsumer
interface Lib2TestServiceA {
    fun method1(str: String)

    fun method2(str: String): String?
    fun methodWithInt(a: Int): Int?
    fun methodWithBoolean(a: Boolean): Boolean?
    fun methodWithDouble(a: Double): Double?
    fun methodWithCallback(a: (Array<JSValue>) -> String): Double?
    fun methodWithArrayString(a: Array<String>): Array<String>?
    fun methodWithMap(a: Map<String, Any?>): Map<String, Any?>?
    fun methodWithUnit(): Unit
    fun methodWithJSValue(a: JSValue): JSValue?
    fun methodWithArrayBuffer(a: ArrayBuffer): ArrayBuffer?
    fun method3Params(a: String, b: Int, c: JSValue): JSValue?
}