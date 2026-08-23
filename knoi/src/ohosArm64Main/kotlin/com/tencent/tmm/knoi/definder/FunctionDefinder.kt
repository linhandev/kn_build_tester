package com.tencent.tmm.knoi.definder

import com.tencent.tmm.knoi.converter.convertJSCallbackInfoToKTParamList
import com.tencent.tmm.knoi.converter.jsValueToKTValue
import com.tencent.tmm.knoi.converter.ktValueToJSValue
import com.tencent.tmm.knoi.exception.FunctionNotRegisterException
import com.tencent.tmm.knoi.exception.ReturnTypeErrorException
import com.tencent.tmm.knoi.injectEnv
import com.tencent.tmm.knoi.logger.debug
import com.tencent.tmm.knoi.logger.info
import com.tencent.tmm.knoi.metric.trace
import com.tencent.tmm.knoi.napi.defineFunctionToExport
import com.tencent.tmm.knoi.napi.safeCaseNumberType
import com.tencent.tmm.knoi.register.FunctionBindInfo
import com.tencent.tmm.knoi.register.FunctionRegister
import com.tencent.tmm.knoi.register.ThreadSafeFunctionRegister
import kotlinx.cinterop.get
import kotlinx.cinterop.staticCFunction
import platform.ohos.knoi.getCbInfoWithSize
import platform.ohos.napi_callback_info
import platform.ohos.napi_env
import platform.ohos.napi_value
import kotlin.reflect.KClass

const val INVOKE_METHOD_NAME = "invoke"

internal val functionRegister = FunctionRegister()
val tsfnRegister = ThreadSafeFunctionRegister()
internal fun registerForwarder(env: napi_env, export: napi_value) {
    defineFunctionToExport(env, export, INVOKE_METHOD_NAME, staticCFunction(::forwardFunction))
    info("registerForwarder successful.")
}

fun bind(
    name: String,
    function: (args: Array<out Any?>) -> Any,
    returnType: KClass<out Any> = Unit::class,
    vararg paramsTypes: KClass<out Any>
) {
    info("bind name = $name. returnType = ${returnType.simpleName}, paramsTypesSize : ${paramsTypes.size}")
    functionRegister.register(FunctionBindInfo(name, function, returnType, paramsTypes))
}

fun unBind(
    name: String
) {
    info("unBind name = $name.")
    functionRegister.unRegister(name)
}

internal fun getForwardFunctionName(env: napi_env?, callbackInfo: napi_callback_info?): String {
    val params = getCbInfoWithSize(env, callbackInfo, 1) ?: error("unknown params.")
    if (params[0] == null) {
        throw IllegalArgumentException("The first parameter must be the function name.")
    }
    return jsValueToKTValue(env, params[0], String::class) as String
}

internal fun forwardFunction(env: napi_env?, callbackInfo: napi_callback_info?): napi_value? {
    injectEnv(env)
    val methodName = getForwardFunctionName(env, callbackInfo)
    info("forwardFunction method = $methodName")
    val paramsTypes = functionRegister.getParamTypes(methodName)
    val paramsValue =
        convertJSCallbackInfoToKTParamList<Any>(env, callbackInfo, paramsTypes?.toList(), 1)
    val func = functionRegister.getFunction(methodName)
    val returnType = functionRegister.getReturnType(methodName)
    if (func == null) {
        throw FunctionNotRegisterException(methodName)
    }
    if (returnType == null) {
        throw ReturnTypeErrorException()
    }

    val expectedSize = paramsTypes?.size ?: 0
    if (paramsValue.size != expectedSize) {
        throw IllegalArgumentException(
            "forwardFunction method = $methodName " + "params length error: expect $expectedSize actual ${paramsValue.size}"
        )
    }
    val transformParamValues = paramsValue.mapIndexed { index: Int, param: Any? ->
        return@mapIndexed safeCaseNumberType(param, paramsTypes!![index])
    }.toTypedArray()
    return trace("forwardFunction method = $methodName ") {
        val resultNApiValue: napi_value?
        debug("forwardFunction method = $methodName , params = ${transformParamValues.contentToString()}")
        val result = func(transformParamValues)
        debug("forwardFunction method = $methodName , result = $result")
        resultNApiValue = ktValueToJSValue(env, result, returnType)
        resultNApiValue
    }
}