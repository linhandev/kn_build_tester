package com.tencent.tmm.knoi.sample

import com.tencent.tmm.knoi.annotation.ServiceConsumer
import com.tencent.tmm.knoi.type.ArrayBuffer
import com.tencent.tmm.knoi.type.JSValue

typealias StringFormat = (String) -> String

@ServiceConsumer
interface TestServiceA {
    // 传入 Int 返回 Int
    fun methodWithIntReturnInt(a: Int): Int

    // 传入 Long 返回 Long
    fun methodWithLongReturnLong(a: Long): Long

    // 传入 Boolean 返回 Boolean
    fun methodWithBooleanReturnBoolean(a: Boolean): Boolean

    // 传入 Double 返回 Double
    fun methodWithDoubleReturnDouble(a: Double): Double

    // 传入 String 返回 String
    fun methodWithStringReturnString(a: String): String?

    // 传入 Callback 返回 Callback
    fun methodWithCallbackReturnCallback(a: (Array<JSValue>) -> Unit): ((Array<JSValue>) -> String)

    // 传入 自定义回调 返回 自定义回调
    fun methodWithCallbackReturnCallback2(
        a: (String, Double, Long, Int, Boolean) -> String,
        b: String,
        c: Int
    ): StringFormat

    // 传入 自定义回调 返回 String
    fun methodWithCallbackReturnCallback3(a: (String, Int) -> String, b: (String, Int) -> String): String

    fun methodWithCallbackReturnCallback4(a: StringFormat): String

    // 传入 String 数组 返回 String 数组
    fun methodWithArrayStringReturnArrayString(a: Array<String>): Array<String>

    // 传入 JS 对象 返回 JS 对象
    fun methodWithMapReturnMap(a: Map<String, Any?>): Map<String, Any?>

    // 无参
    fun methodWithUnit()

    // 传入 任意 JS 类型，返回任意 JS类型
    fun methodWithJSValueReturnJSValue(a: JSValue): JSValue

    // 传入 ArrayBuffer 二进制数据，返回 二进制数据
    fun methodWithArrayBufferReturnArrayBuffer(a: ArrayBuffer): ArrayBuffer?

    // 传入 ArrayBuffer 二进制数据，返回 二进制数据 使用 ByteArray
    fun methodWithArrayBufferReturnArrayBufferUseByteArray(a: ArrayBuffer): ArrayBuffer

    // 多参数
    fun method3Params(a: String, b: Int, c: JSValue): JSValue

    // 异常处理
    fun methodWithException(a: String): JSValue

    // 返回类型为 Promise
    fun methodWithPromise(a:String): JSValue
}