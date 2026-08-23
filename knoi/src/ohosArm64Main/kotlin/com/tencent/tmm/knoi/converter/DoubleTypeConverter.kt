package com.tencent.tmm.knoi.converter

import platform.ohos.knoi.convertDoubleToNapiValue
import platform.ohos.knoi.toDouble
import platform.ohos.napi_env
import platform.ohos.napi_value
import platform.ohos.napi_valuetype
import kotlin.reflect.KClass

class DoubleTypeConverter : TypeConverter<Double> {
    override fun convertJSValueToKotlinValue(env: napi_env?, value: napi_value?): Double? {
        return toDouble(env, value)
    }

    override fun convertKotlinValueToJSValue(env: napi_env?, value: Double?): napi_value? {
        if (value == null) {
            return null
        }
        return convertDoubleToNapiValue(env, value)
    }

    override fun getJSType(): napi_valuetype {
        return napi_valuetype.napi_number
    }

    override fun getKType(): KClass<out Any> = Double::class
}