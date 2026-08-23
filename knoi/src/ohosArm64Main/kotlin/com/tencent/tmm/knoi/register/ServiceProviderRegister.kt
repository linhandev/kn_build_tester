package com.tencent.tmm.knoi.register

import com.tencent.tmm.knoi.converter.ktValueToJSValue
import com.tencent.tmm.knoi.definder.serviceProviderRegister
import com.tencent.tmm.knoi.exception.ServiceNotRegisterException
import com.tencent.tmm.knoi.getEnv
import com.tencent.tmm.knoi.service.Invokable
import com.tencent.tmm.knoi.type.JSValue
import kotlinx.atomicfu.locks.synchronized
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.staticCFunction
import platform.ohos.knoi.wrap
import platform.ohos.napi_env

/**
 * 获取静态对象
 * @param proxyJSValue JavaScript 对象
 * @param name 服务名字
 */
fun ServiceProviderRegister.getInvokable(proxyJSValue: JSValue, name: String): Invokable {
    synchronized(this) {
        val hash = proxyJSValue["hash"]
        if (hash != null && !hash.isUndefined() && hashToServiceProxy[hash.toKString()] != null) {
            return hashToServiceProxy[proxyJSValue["hash"]!!.toKString()]!!
        }

        val service = getServiceByName(name) ?: throw ServiceNotRegisterException("name = $name")
        registerServiceProxy(name, proxyJSValue, service)
        return service
    }
}

private fun ServiceProviderRegister.registerServiceProxy(name: String, value: JSValue, invokable: Invokable) {
    val hash = invokable.hashCode().toString()
    hashToServiceProxy[hash] = invokable
    value["hash"] = JSValue(ktValueToJSValue(getEnv(), hash, String::class))
    // 注册 gc 监听
    wrap(
        getEnv(),
        value.handle,
        StableRef.create(invokable).asCPointer(),
        staticCFunction(::finalizeInvokableProxy)
    )
}

fun finalizeInvokableProxy(
    env: napi_env, data: COpaquePointer, hint: COpaquePointer
) {
    val ref = data.asStableRef<Invokable>()
    serviceProviderRegister.releaseInvokable(ref.get())
    ref.dispose()
}