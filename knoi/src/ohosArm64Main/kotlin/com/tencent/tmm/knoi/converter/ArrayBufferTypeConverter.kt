package com.tencent.tmm.knoi.converter

import com.tencent.tmm.knoi.type.ArrayBuffer
import platform.ohos.knoi.isArrayBuffer
import platform.ohos.knoi.isTypedArray
import platform.ohos.napi_env
import platform.ohos.napi_value
import platform.ohos.napi_valuetype
import kotlin.reflect.KClass

/**
 * ArrayBuffer 直接操作 napi_value 的 指针，不存在数据拷贝
 * 无法映射为 Kotlin ByteArray，是因为 ByteArray 存在数据拷贝
 */
open class ArrayBufferTypeConverter : TypeConverter<ArrayBuffer> {
    override fun convertJSValueToKotlinValue(env: napi_env?, value: napi_value?): ArrayBuffer? {
        return ArrayBuffer(value)
    }

    override fun convertKotlinValueToJSValue(env: napi_env?, value: ArrayBuffer?): napi_value? {
        if (value == null) {
            return null
        }
        return value.handle
    }

    override fun isSupportJSType(env: napi_env?, type: napi_valuetype, value: napi_value?): Boolean {
        return isArrayBuffer(env, value) || isTypedArray(env, value)
    }

    override fun isSupportKType(type: KClass<out Any>): Boolean {
        return type == getKType()
    }

    override fun getKType(): KClass<out Any> {
        return ArrayBuffer::class
    }
}