package com.tencent.tmm.knoi.converter

import platform.ohos.napi_env
import platform.ohos.napi_value
import kotlin.reflect.KClass

class UnitTypeConverter : TypeConverter<Unit> {
    override fun convertJSValueToKotlinValue(env: napi_env?, value: napi_value?) {
    }

    override fun convertKotlinValueToJSValue(env: napi_env?, value: Unit?): napi_value? {
        return null
    }

    override fun getKType(): KClass<out Any> = Unit::class
}