package com.tencent.tmm.knoi.benchmark

import com.tencent.tmm.knoi.annotation.ServiceConsumer
import com.tencent.tmm.knoi.type.ArrayBuffer
import com.tencent.tmm.knoi.type.JSCallback
import com.tencent.tmm.knoi.type.JSValue

/**
 * Bencval
 *
 * converters = arrayOf(
 *     // 无类型辅助推导的情况下，依赖顺序
 *     UnitTypeConverter(),
 *     BooleanTypeConverter(),
 *     IntTypeConverter(),
 *     LongTypeConverter(),
 *     DoubleTypeConverter(),
 *     StringTypeConverter(),
 *     ArrayBufferTypeConverter(),
 *     ArrayTypeConverter(),
 *     ListTypeConverter(),
 *     JSCallbackTypeConverter(),
 *     MapTypeConverter(),
 *
 *     // 上面添加实现，JSValue 为 Any。
 *     JSValueTypeConverter()
 * )
 * h mark consumer
 *
 * @constructor Create empty Bench mark consumer
 */
@ServiceConsumer
interface BenchMarkConsumer {

    fun benchUnit()

    fun benchBoolean(boolean: Boolean):Boolean

    fun benchInt(int: Int):Int

    fun benchLong(long: Long):Long

    fun benchDouble(double: Double):Double

    fun benchString(string: String): String

    fun benchArrayBuffer():ArrayBuffer

    fun benchList(list: List<Any>):List<Any>

    fun benchJSCallback(jsCallback: (Array<JSValue>) -> Any?):(Array<JSValue>) -> Any?

    fun benchMap(map: Map<String, Any>): Map<String,Any>
}

@ServiceConsumer
interface BenchMarkConsumerForWord {

    fun benchUnit()

    fun benchBoolean(boolean: Boolean)

    fun benchInt(int: Int)

    fun benchLong(long: Long)

    fun benchDouble(double: Double)

    fun benchString(string: String)

    fun benchList(list: List<Any>)

    fun benchJSCallback(jsCallback: (Array<JSValue>) -> Any?)

    fun benchMap(map: Map<String, Any>)

}