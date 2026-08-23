package com.tencent.tmm.knoi.jsbind

import com.tencent.tmm.knoi.converter.jsValueToKTValue
import com.tencent.tmm.knoi.converter.ktValueToJSValue
import com.tencent.tmm.knoi.injectEnv
import com.tencent.tmm.knoi.logger.info
import com.tencent.tmm.knoi.napi.defineFunctionToExport
import com.tencent.tmm.knoi.register.JSFunctionRegister
import kotlinx.cinterop.get
import kotlinx.cinterop.staticCFunction
import platform.ohos.knoi.getCbInfoWithSize
import platform.ohos.knoi.typeOf
import platform.ohos.napi_callback_info
import platform.ohos.napi_env
import platform.ohos.napi_value
import platform.ohos.napi_valuetype

const val JS_BIND_METHOD_NAME = "bind"
const val JS_UNBIND_METHOD_NAME = "unBind"

internal val jsFunctionRegister = JSFunctionRegister()

internal fun registerBindJSFunction(env: napi_env, export: napi_value) {
    defineFunctionToExport(env, export, JS_BIND_METHOD_NAME, staticCFunction(::bindJSFunction))
    defineFunctionToExport(env, export, JS_UNBIND_METHOD_NAME, staticCFunction(::unBindJSFunction))
    info("registerBindFunction successful.")
}

internal fun unBindJSFunction(env: napi_env?, callbackInfo: napi_callback_info?): napi_value? {
    val params = getCbInfoWithSize(env, callbackInfo, 1) ?: error("unknown params.")
    if (params[0] == null) {
        return null
    }
    val methodName = jsValueToKTValue(env, params[0], String::class)
    if (methodName !is String || methodName.isEmpty()) {
        throw IllegalArgumentException("The first parameter must be the function name.")
    }
    info("unBindJSFunction method = $methodName")
    jsFunctionRegister.unregister(methodName)
    return ktValueToJSValue(env, true, Boolean::class)
}

internal fun bindJSFunction(env: napi_env?, callbackInfo: napi_callback_info?): napi_value? {
    injectEnv(env)
    val params = getCbInfoWithSize(env, callbackInfo, 2) ?: error("unknown params.")
    if (params[0] == null || params[1] == null) {
        return null
    }
    val methodName = jsValueToKTValue(env, params[0], String::class)
    if (methodName !is String || methodName.isEmpty()) {
        throw IllegalArgumentException("The first parameter must be the function name.")
    }
    val funcValue = params[1]
    if (typeOf(env, funcValue) != napi_valuetype.napi_function) {
        throw IllegalArgumentException("The second parameter must be a function.")
    }
    info("bindJSFunction method = $methodName")
    val jsFunction = JSFunction(env, methodName, funcValue)
    jsFunctionRegister.register(jsFunction)
    return ktValueToJSValue(env, true, Boolean::class)
}


fun getJSFunction(name: String): JSFunction? {
    return jsFunctionRegister.getJSFunction(name)
}

