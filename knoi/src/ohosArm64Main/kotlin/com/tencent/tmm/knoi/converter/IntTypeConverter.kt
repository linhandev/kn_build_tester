package com.tencent.tmm.knoi.converter

import platform.ohos.knoi.convertIntToNapiValue
import platform.ohos.knoi.toInt
import platform.ohos.napi_env
import platform.ohos.napi_value
import kotlin.reflect.KClass

class IntTypeConverter : TypeConverter<Int> {
    override fun convertJSValueToKotlinValue(env: napi_env?, value: napi_value?): Int {
        return toInt(env, value)
    }

    override fun getKType(): KClass<out Any> = Int::class

    override fun convertKotlinValueToJSValue(env: napi_env?, value: Int?): napi_value? {
        if (value == null) {
            return null
        }
        return convertIntToNapiValue(env, value)
    }
}