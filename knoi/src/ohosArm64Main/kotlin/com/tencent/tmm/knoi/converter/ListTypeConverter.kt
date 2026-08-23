package com.tencent.tmm.knoi.converter

import platform.ohos.knoi.isArray
import platform.ohos.napi_env
import platform.ohos.napi_value
import platform.ohos.napi_valuetype
import kotlin.reflect.KClass

class ListTypeConverter : TypeConverter<List<Any?>> {
    private val arrayTypeConverter = ArrayTypeConverter()
    private val emptyListKClass = emptyList<Nothing>()::class
    override fun convertJSValueToKotlinValue(env: napi_env?, value: napi_value?): List<Any?>? {
        return arrayTypeConverter.convertJSValueToKotlinValue(env, value)?.toList()
    }

    override fun convertKotlinValueToJSValue(env: napi_env?, value: List<Any?>?): napi_value? {
        return arrayTypeConverter.convertKotlinValueToJSValue(env, value?.toTypedArray())
    }

    override fun isSupportJSType(
        env: napi_env?, type: napi_valuetype, value: napi_value?
    ): Boolean {
        return isArray(env, value)
    }

    override fun isSupportKType(type: KClass<out Any>): Boolean {
        return type == getKType() || type == ArrayList::class || type == emptyListKClass
    }

    override fun getKType(): KClass<out Any> {
        return List::class
    }

}