package com.tencent.tmm.knoi.converter

import platform.ohos.knoi.createArray
import platform.ohos.knoi.getArrayLength
import platform.ohos.knoi.getElementInArray
import platform.ohos.knoi.setElementInArray
import platform.ohos.knoi.typeOf
import platform.ohos.napi_env
import platform.ohos.napi_value
import platform.ohos.napi_valuetype
import kotlin.reflect.KClass

open class ArrayTypeConverter : TypeConverter<Array<Any?>> {
    override fun convertJSValueToKotlinValue(env: napi_env?, value: napi_value?): Array<Any?>? {
        if (typeOf(env, value) == napi_valuetype.napi_undefined) {
            return null
        }
        val length = getArrayLength(env, value).toInt()
        val result = Array<Any?>(length) {}
        if (length == 0) {
            return result
        }
        for (index in 0 until length) {
            val element = getElementInArray(env, value, index)
            val ktElement =
                getFirstSupportConverter(env, element).convertJSValueToKotlinValue(env, element)
            result[index] = (ktElement)
        }
        return result
    }

    override fun convertKotlinValueToJSValue(env: napi_env?, value: Array<Any?>?): napi_value? {
        if (value == null) {
            return null
        }
        val array = createArray(env) ?: return null
        value.forEachIndexed { index, element ->
            val jsElement = if (element == null) {
                null
            } else {
                ktValueToJSValue(env, element, element::class)
            }
            setElementInArray(env, array, index, jsElement)
        }
        return array
    }

    override fun isSupportKType(type: KClass<out Any>): Boolean {
        return type == getKType()
    }

    override fun getKType(): KClass<out Any> {
        return Array::class
    }


}