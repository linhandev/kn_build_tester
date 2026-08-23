@file:Suppress("UNCHECKED_CAST")

package com.tencent.tmm.knoi.converter

import com.tencent.tmm.knoi.exception.UnSupportTypeException
import com.tencent.tmm.knoi.logger.debug
import com.tencent.tmm.knoi.metric.trace
import com.tencent.tmm.knoi.type.JSValue
import kotlinx.cinterop.get
import platform.ohos.knoi.getCallbackInfoParamsSize
import platform.ohos.knoi.getCbInfoWithSize
import platform.ohos.knoi.typeOf
import platform.ohos.napi_callback_info
import platform.ohos.napi_env
import platform.ohos.napi_value
import platform.ohos.napi_valuetype
import platform.posix.free
import kotlin.reflect.KClass

val converters = arrayOf(
    // 无类型辅助推导的情况下，依赖顺序
    UnitTypeConverter(),
    BooleanTypeConverter(),
    IntTypeConverter(),
    LongTypeConverter(),
    DoubleTypeConverter(),
    StringTypeConverter(),
    ArrayBufferTypeConverter(),
    ArrayTypeConverter(),
    ListTypeConverter(),
    JSCallbackTypeConverter(),
    MapTypeConverter(),

    // 上面添加实现，JSValue 为 Any。
    JSValueTypeConverter()
)

/**
 * 将 Kotlin 类型 转为 JavaScript 类型
 * 转换规则见 “类型转换”章节
 */
fun ktValueToJSValue(env: napi_env?, value: Any?, clazz: KClass<out Any>): napi_value? {
    if (env == null || value == null) {
        return null
    }
    val typeConverter = getFirstSupportConverter(clazz)
    return typeConverter.convertKotlinValueToJSValueWithAny(env, value)
}

/**
 * 将 JavaScript 类型转 Kotlin 类型
 * 转换规则见 “类型转换”章节
 */
fun jsValueToKTValue(
    env: napi_env?, value: napi_value?, kType: KClass<out Any> = Any::class
): Any? {
    if (env == null || value == null) {
        return null
    }
    if (kType == Unit::class) {
        return Unit
    }
    val type = typeOf(env, value)
    if (type == napi_valuetype.napi_undefined || type == napi_valuetype.napi_null) {
        return null
    }
    return getFirstSupportConverter(kType).convertJSValueToKotlinValue(env, value)
}

/**
 * 获取 支持转换 type 类型的类型转换器
 * @param type Kotlin 类型
 * @return 类型转换器
 */
fun getFirstSupportConverter(type: KClass<out Any>): TypeConverter<out Any> {
    val typeConverter = converters.find {
        return@find it.isSupportKType(type)
    }
    if (typeConverter != null) {
        return typeConverter
    }
    if (type.toString().contains("$") || type.toString().contains("kotlin.Function")) {
        return getFirstSupportConverter(Function::class)
    }
    throw UnSupportTypeException("kotlin type = ${type.toString()} ${type.simpleName}")

}

/**
 * 获取 支持转换 napi_value 类型的类型转换器
 * @param value napi_value
 * @return 类型转换器
 */
fun getFirstSupportConverter(env: napi_env?, value: napi_value?): TypeConverter<out Any> {
    val type = typeOf(env, value)
    val typeConverter = converters.find {
        return@find it.isSupportJSType(env, type, value)
    }

    if (typeConverter == null) {
        throw UnSupportTypeException("js type = $type")
    } else {
        return typeConverter
    }
}

fun <T> convertJSCallbackInfoToKTParamList(
    env: napi_env?,
    callbackInfo: napi_callback_info?,
    paramsType: List<KClass<out Any>>? = null,
    offset: Int = 0
): MutableList<T?> {
    return trace("TypeConvertCenter:convertJSCallbackInfoToKTParamList") {
        val jsParamsSize = getCallbackInfoParamsSize(env, callbackInfo)
        val params = getCbInfoWithSize(env, callbackInfo, jsParamsSize) ?: error("unknown params.")

        val paramsValue = mutableListOf<T?>()
        try {

            for (index in offset until jsParamsSize) {
                val kType = if (paramsType != null) {
                    paramsType[index - offset]
                } else {
                    JSValue::class
                }
                val ktValue = jsValueToKTValue(env, params[index], kType)
                paramsValue.add(ktValue as T?)
            }

        } finally {
            free(params)
        }
        paramsValue
    }

}