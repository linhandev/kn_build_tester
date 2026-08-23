package com.tencent.tmm.knoi.jsbind

import com.tencent.tmm.knoi.definder.tsfnRegister
import com.tencent.tmm.knoi.napi.safeCaseNumberType
import com.tencent.tmm.knoi.type.JSValue
import platform.ohos.knoi.get_tid
import platform.ohos.napi_env
import platform.ohos.napi_value
import kotlin.reflect.KClass

class JSFunction(val env: napi_env?, val name: String, jsCallback: napi_value?) {
    var recvJSValue: JSValue? = null
    val bindTid: Int = get_tid()
    val jsValue: JSValue = JSValue(env, jsCallback, bindTid)

    /**
     * bind This，JS 闭包 则可通过 this.XXX 访问
     */
    fun bind(env: napi_env?, recv: napi_value?) {
        recvJSValue = JSValue(env, recv, bindTid)
    }

    inline fun <reified T : Any> invoke(vararg params: Any?): T? {
        val result = tsfnRegister.callSyncSafe(bindTid) {
            return@callSyncSafe invokeDirect(params, T::class)
        }
        return safeCaseNumberType(result, T::class) as T?
    }

    /**
     * Invoke direct
     * 避免旧版本inline调不通
     *
     * @param params
     * @param kType
     * @return
     */
    fun invokeDirect(params: Array<out Any?>, kType: KClass<out Any>): Any? {
        return invokeDirect(params, kType, jsValue, recvJSValue)
    }

}