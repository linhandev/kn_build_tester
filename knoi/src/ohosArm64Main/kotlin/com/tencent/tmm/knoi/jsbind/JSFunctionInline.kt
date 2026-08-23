package com.tencent.tmm.knoi.jsbind

import com.tencent.tmm.knoi.converter.jsValueToKTValue
import com.tencent.tmm.knoi.converter.ktValueToJSValue
import com.tencent.tmm.knoi.exception.JavaScriptException
import com.tencent.tmm.knoi.getEnv
import com.tencent.tmm.knoi.type.JSValue
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.value
import platform.ohos.knoi.callFunction
import platform.ohos.napi_value
import platform.ohos.napi_valueVar
import kotlin.reflect.KClass


/**
 * warming: 只能在 JS 含有 napi_env 线程调用
 */

inline fun invokeDirect(
    params: Array<out Any?>,
    kType: KClass<out Any>,
    jsCallback: JSValue,
    recvJSValue: JSValue?,
): Any? {
    val paramsNapiValue = convertParamsToJSValueArray(params)
    val jsCb = jsCallback.handle
    val recv = recvJSValue?.handle
    memScoped {
        val exceptionVar = alloc<napi_valueVar>()
        val result = callFunction(
            getEnv(),
            recv,
            jsCb,
            paramsNapiValue.size,
            paramsNapiValue.toTypedArray().toCValues(),
            exceptionVar.ptr
        )
        val exceptionValue = exceptionVar.value
        if (exceptionValue == null) {
            return jsValueToKTValue(getEnv(), result, kType)
        } else {
            throw JavaScriptException(exceptionValue)
        }
    }
}

inline fun convertParamsToJSValueArray(params: Array<out Any?>): List<napi_value?> {
    if (params.isEmpty()) {
        return emptyList()
    }
    return params.map {
        if (it == null) {
            null
        } else {
            ktValueToJSValue(getEnv(), it, it::class)
        }
    }
}
