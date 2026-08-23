package com.tencent.tmm.knoi.converter

import com.tencent.tmm.knoi.type.JSValue
import platform.ohos.knoi.createObject
import platform.ohos.knoi.getArrayLength
import platform.ohos.knoi.getElementInArray
import platform.ohos.knoi.getPropertyNames
import platform.ohos.knoi.getPropertyValue
import platform.ohos.knoi.setPropertyValue
import platform.ohos.knoi.isInstanceOf
import platform.ohos.knoi.typeOf
import platform.ohos.napi_env
import platform.ohos.napi_value
import platform.ohos.napi_valuetype
import kotlin.reflect.KClass

/**
 * Map 类型映射未 JavaScript 的 Object 类型
 */
class MapTypeConverter : TypeConverter<Map<String, Any?>> {

    private val emptyMapKClass = emptyMap<Nothing, Nothing>()::class

    override fun convertJSValueToKotlinValue(
        env: napi_env?, value: napi_value?
    ): Map<String, Any?> {
        return if (isJSMap(env, value)) {
            convertJSMapToKotlinValue(env, value)
        } else {
            convertJSObjectToKotlinValue(env, value)
        }
    }

    private fun isJSMap(env: napi_env?, value: napi_value?): Boolean {
        return isInstanceOf(env,  JSValue.global()["Map"]?.handle, value)
    }

    private fun convertJSMapToKotlinValue(
        env: napi_env?, value: napi_value?
    ): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val jsMap = JSValue(value)
        val keysIterator = jsMap.callMethod<JSValue>("keys")!!
        while (true) {
            val keyValuePair = keysIterator.callMethod<JSValue>("next") ?: continue
            val done = keyValuePair["done"] ?: continue

            if (done.isBoolean() && done.toBoolean()) {
                break
            }
            val key = keyValuePair["value"]?.toKString() ?: continue
            val valueOfKey = jsMap.callMethod<JSValue>("get", key)
            if (valueOfKey?.handle != null) {
                val converter = getFirstSupportConverter(env, valueOfKey.handle)
                val ktValue = converter.convertJSValueToKotlinValue(env, valueOfKey.handle)
                map[key] = ktValue
            } else {
                map[key] = null
            }

        }
        return map.toMap()
    }

    private fun convertJSObjectToKotlinValue(
        env: napi_env?, value: napi_value?
    ): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        val names = getPropertyNames(env, value)
        val length = getArrayLength(env, names).toInt()
        for (i in 0 until length) {
            val key = getElementInArray(env, names, i)
            val keyStr = jsValueToKTValue(env, key, String::class) as String
            val valueOfKey = getPropertyValue(env, value, keyStr)
            val converter = getFirstSupportConverter(env, valueOfKey)
            val ktValue = converter.convertJSValueToKotlinValue(env, valueOfKey)
            map[keyStr] = ktValue
        }
        return map.toMap()
    }

    override fun isSupportKType(type: KClass<out Any>): Boolean {
        // 由于 K/N 支持的反射能力有限，无法判断KClass 继承关系
        return Map::class == type || getKType() == type || type == emptyMapKClass
    }

    override fun getJSType(): napi_valuetype {
        return napi_valuetype.napi_object
    }

    override fun getKType(): KClass<out Any> = HashMap::class

    override fun convertKotlinValueToJSValue(
        env: napi_env?, value: Map<String, Any?>?
    ): napi_value? {
        if (value == null) {
            return null
        }
        val obj = createObject(env)
        value.forEach {
            val key = it.key
            val valueOfKey = it.value ?: return@forEach
            val typeConverter = getFirstSupportConverter(valueOfKey::class)
            val napiValue = ktValueToJSValue(env, valueOfKey, typeConverter.getKType())
            setPropertyValue(env, obj, key, napiValue)
        }
        return obj
    }
}