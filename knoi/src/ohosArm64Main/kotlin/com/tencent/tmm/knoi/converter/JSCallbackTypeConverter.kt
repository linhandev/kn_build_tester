package com.tencent.tmm.knoi.converter

import com.tencent.tmm.knoi.collections.SafeHashMap
import com.tencent.tmm.knoi.jsbind.JSFunction
import com.tencent.tmm.knoi.logger.debug
import com.tencent.tmm.knoi.logger.isDebug
import com.tencent.tmm.knoi.type.JSValue
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.staticCFunction
import platform.ohos.knoi.createFunction
import platform.ohos.knoi.getCbInfoData
import platform.ohos.knoi.wrap
import platform.ohos.napi_callback_info
import platform.ohos.napi_env
import platform.ohos.napi_value
import platform.ohos.napi_valuetype
import kotlin.reflect.KClass

val ktFunctionMap = SafeHashMap<String, CallbackWrapper>()

class CallbackWrapper(var callback: ((args: Array<JSValue?>) -> Any?)? = null)

fun Any.knoiDispose() {
    ktFunctionMap.remove(this.hashCode().toString())?.callback = null
}

class JSCallbackTypeConverter : TypeConverter<(args: Array<JSValue?>) -> Any?> {
    override fun convertJSValueToKotlinValue(
        env: napi_env?,
        value: napi_value?
    ): (Array<JSValue?>) -> Any? {
        val jsFunction = JSFunction(env, "", value)
        val funcWrapper: (args: Array<out Any?>) -> Any? = { args ->
            jsFunction.invoke<JSValue>(*(args))
        }
        return funcWrapper
    }

    override fun getJSType(): napi_valuetype {
        return napi_valuetype.napi_function
    }

    override fun getKType(): KClass<out Any> = Function::class

    override fun convertKotlinValueToJSValue(
        env: napi_env?,
        value: ((args: Array<JSValue?>) -> Any?)?
    ): napi_value? {
        if (value == null) {
            return null
        }
        val callbackWrapper = CallbackWrapper(value)
        val hashCode = value.hashCode().toString()
        val ptr = StableRef.create(callbackWrapper).asCPointer()
        ktFunctionMap.put(hashCode, callbackWrapper)

        // 仅 debug 需要排查泄露使用，采用统一名字减少字符串占用，
        val functionName = if (isDebug) "JSCallback@${hashCode}" else "JSCallback"
        // 采用 napi_create_function 创建匿名函数
        val jsFunction = createFunction(
            env,
            functionName,
            staticCFunction(::forwardJSCallback),
            ptr
        )

        wrap(
            env,
            jsFunction,
            ptr,
            staticCFunction(::finalizeJSCallback)
        )
        return jsFunction
    }
}

fun finalizeJSCallback(
    env: napi_env, data: COpaquePointer, hint: COpaquePointer
) {
    val ref = data.asStableRef<CallbackWrapper>()
    val callbackWrapper = ref.get()
    callbackWrapper.callback?.knoiDispose()
    ref.dispose()
}

internal fun forwardJSCallback(env: napi_env?, callbackInfo: napi_callback_info?): napi_value? {
    val ptr: COpaquePointer = getCbInfoData(env, callbackInfo) ?: return null
    val ref = ptr.asStableRef<CallbackWrapper>()
    val func = try {
        ref.get().callback
    } catch (e: Exception) {
        null
    }
    val result =
        func?.invoke(convertJSCallbackInfoToKTParamList<JSValue>(env, callbackInfo).toTypedArray())
            ?: return null
    val typeConverter = getFirstSupportConverter(result::class)
    return ktValueToJSValue(env, result, typeConverter.getKType())
}


fun dumpAllJSCallback() {
    if (!isDebug) {
        return
    }
    debug("JSCallback alive size: ${ktFunctionMap.size()}")
    if (ktFunctionMap.size() == 0) {
        return
    }
    var num = 1
    debug("------- JSCallback alive list begin --------")
    ktFunctionMap.forEach {
        val func = it.value
        debug("No ${num}: name=$func hash=${it.key}")
        num++
    }
    debug("------- JSCallback alive list end --------")
}