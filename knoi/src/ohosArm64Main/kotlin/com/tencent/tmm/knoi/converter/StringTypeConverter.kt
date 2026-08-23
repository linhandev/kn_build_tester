package com.tencent.tmm.knoi.converter

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.toKString
import platform.ohos.knoi.convertStringToNapiValue
import platform.ohos.knoi.toKString
import platform.ohos.knoi.typeOf
import platform.ohos.napi_env
import platform.ohos.napi_value
import platform.ohos.napi_valuetype
import platform.posix.free
import kotlin.reflect.KClass

class StringTypeConverter : TypeConverter<String> {
    override fun convertJSValueToKotlinValue(env: napi_env?, value: napi_value?): String? {
        if (value == null || typeOf(env, value) == napi_valuetype.napi_undefined) {
            return null
        }
        val methodNameCharArray: CPointer<ByteVar> = toKString(env, value)
            ?: return null
        val valueStr = methodNameCharArray.toKString()
        free(methodNameCharArray)
        return valueStr
    }

    override fun getKType(): KClass<out Any> = String::class

    override fun getJSType(): napi_valuetype {
        return napi_valuetype.napi_string
    }

    override fun convertKotlinValueToJSValue(env: napi_env?, value: String?): napi_value? {
        if (value == null) {
            return null
        }
        return convertStringToNapiValue(env, value)
    }
}