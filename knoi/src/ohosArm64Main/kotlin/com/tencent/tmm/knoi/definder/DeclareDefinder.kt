package com.tencent.tmm.knoi.definder

import com.tencent.tmm.knoi.converter.jsValueToKTValue
import com.tencent.tmm.knoi.converter.ktValueToJSValue
import com.tencent.tmm.knoi.exception.DeclareNotRegisterException
import com.tencent.tmm.knoi.injectEnv
import com.tencent.tmm.knoi.logger.debug
import com.tencent.tmm.knoi.logger.info
import com.tencent.tmm.knoi.napi.defineFunctionToExport
import com.tencent.tmm.knoi.register.DeclareInfo
import com.tencent.tmm.knoi.register.DeclareRegister
import kotlinx.cinterop.get
import kotlinx.cinterop.staticCFunction
import platform.ohos.knoi.getCbInfoWithSize
import platform.ohos.napi_callback_info
import platform.ohos.napi_env
import platform.ohos.napi_value

const val GET_DECLARE_METHOD_NAME: String = "getDeclare"
val declareRegister = DeclareRegister()

/**
 * 注册常量 forwarder
 * @param env JS 环境
 * @param export 注入的对象
 */
internal fun registerDeclareForwarder(env: napi_env, export: napi_value) {
    defineFunctionToExport(env, export, GET_DECLARE_METHOD_NAME, staticCFunction(::getDeclare))
    info("registerDeclareForwarder successful.")
}

/**
 * 注册常量定义
 */
fun <T : Any> registerDeclare(name: String, declare: () -> T) {
    declareRegister.register(DeclareInfo(name, declare))
}

/**
 * 获取常量名字
 */
internal fun getDeclareName(env: napi_env?, callbackInfo: napi_callback_info?): String {
    val params = getCbInfoWithSize(env, callbackInfo, 1) ?: error("unknown params.")
    if (params[0] == null) {
        throw IllegalArgumentException("The first parameter must be the Declare name.")
    }
    return jsValueToKTValue(env, params[0], String::class) as String
}

internal fun getDeclare(env: napi_env?, callbackInfo: napi_callback_info?): napi_value? {
    injectEnv(env)
    val name = getDeclareName(env, callbackInfo)
    debug("getDeclare name = $name")
    val declare = declareRegister.getDeclare(name) ?: throw DeclareNotRegisterException(name)
    return ktValueToJSValue(env, declare.declare.invoke(), String::class)
}