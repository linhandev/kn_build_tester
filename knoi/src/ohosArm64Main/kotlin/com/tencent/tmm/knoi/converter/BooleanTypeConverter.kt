package com.tencent.tmm.knoi.converter

import platform.ohos.knoi.convertBooleanToNapiValue
import platform.ohos.knoi.toBoolean
import platform.ohos.napi_env
import platform.ohos.napi_value
import platform.ohos.napi_valuetype
import kotlin.reflect.KClass

class BooleanTypeConverter : TypeConverter<Boolean> {
    override fun convertJSValueToKotlinValue(env: napi_env?, value: napi_value?): Boolean {
        return toBoolean(env, value)
    }

    override fun convertKotlinValueToJSValue(env: napi_env?, value: Boolean?): napi_value? {
        if (value == null) {
            return null
        }
        return convertBooleanToNapiValue(env, value)
    }

    override fun getKType(): KClass<out Any> = Boolean::class

    override fun getJSType(): napi_valuetype = napi_valuetype.napi_boolean
}