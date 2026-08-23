package com.tencent.tmm.knoi.converter

import com.tencent.tmm.knoi.type.JSValue
import platform.ohos.napi_env
import platform.ohos.napi_value
import platform.ohos.napi_valuetype
import kotlin.reflect.KClass

class JSValueTypeConverter : TypeConverter<JSValue> {
    override fun convertJSValueToKotlinValue(env: napi_env?, value: napi_value?): JSValue =
        JSValue(env, value)

    override fun getKType(): KClass<out Any> = JSValue::class

    override fun convertKotlinValueToJSValue(env: napi_env?, value: JSValue?): napi_value? {
        if (value == null) {
            return null
        }
        return value.handle
    }

    override fun isSupportJSType(
        env: napi_env?,
        type: napi_valuetype,
        value: napi_value?
    ): Boolean {
        return getJSType() == type || napi_valuetype.napi_undefined == type
    }
}