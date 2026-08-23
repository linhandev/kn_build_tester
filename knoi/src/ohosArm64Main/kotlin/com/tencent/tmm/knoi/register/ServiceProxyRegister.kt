package com.tencent.tmm.knoi.register

import com.tencent.tmm.knoi.definder.tsfnRegister
import com.tencent.tmm.knoi.exception.ServiceFactoryFailException
import com.tencent.tmm.knoi.exception.ServiceNotRegisterException
import com.tencent.tmm.knoi.exception.ServiceProxyNotFoundException
import com.tencent.tmm.knoi.metric.trace
import com.tencent.tmm.knoi.type.JSValue
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import platform.ohos.knoi.createReference
import platform.ohos.knoi.getReferenceValue
import platform.ohos.knoi.get_tid
import platform.ohos.knoi.newInstance
import platform.ohos.napi_env
import platform.ohos.napi_ref
import platform.ohos.napi_value

fun ServiceWrapper.nameSingleton(): String {
    return "${this.name}_${this.registerTid}"
}

/**
 *  Service 代理注册表
 */
class ServiceProxyRegister : SynchronizedObject() {
    private val nameToServiceWrapper = mutableMapOf<String, ServiceProxyLoadBalancer>()
    private val nameToServiceProxy = mutableMapOf<String, Any?>()
    private val nameToSingletonService = mutableMapOf<String, JSValue>()

    /**
     * 注册 JavaScript 实现的 Service
     */
    fun register(env: napi_env?, name: String, isSingleton: Boolean, provider: napi_value?) {
        synchronized(this) {
            val providerJSValue = JSValue(provider)
            val serviceProxyLoadBalancer = nameToServiceWrapper[name]
            if (isSingleton && providerJSValue.isObject()) {
                val serviceWrapper = ServiceWrapper(env, name, true, null)
                registerServiceProxyLoadBalancer(serviceProxyLoadBalancer, name, serviceWrapper)
                nameToSingletonService[serviceWrapper.nameSingleton()] = providerJSValue
            } else {
                val serviceWrapper =
                    ServiceWrapper(env, name, isSingleton, createReference(env, provider))
                registerServiceProxyLoadBalancer(serviceProxyLoadBalancer, name, serviceWrapper)
            }
        }
        trace("register Service $name ${if (nameToServiceWrapper[name] != null) "success" else "fail"} .")
    }

    /**
     * 注册负载均衡的 Service
     */
    private fun registerServiceProxyLoadBalancer(
        serviceProxyLoadBalancer: ServiceProxyLoadBalancer?,
        name: String,
        serviceWrapper: ServiceWrapper
    ) {
        if (serviceProxyLoadBalancer == null) {
            nameToServiceWrapper[name] = ServiceProxyLoadBalancer().also {
                it.provide(serviceWrapper)
            }
        } else {
            serviceProxyLoadBalancer.provide(serviceWrapper)
        }
    }

    /**
     * 绑定服务静态代理类，用于转发服务调用代理到 JS 对象
     * @param name 服务名
     * @param proxy 静态代理对象
     */
    internal fun <T> bindProxy(name: String, proxy: T) {
        synchronized(this) {
            nameToServiceProxy[name] = proxy
        }
        trace("bindProxy Service $name success")
    }

    /***
     * 服务调用
     * @param name 服务名
     * @param method 方法名
     * @param params 参数包
     * @return 返回值
     */
    inline fun <reified T> callService(name: String, method: String, vararg params: Any?): T? {
        return trace("callService:$name#$method") {
            val service = getServiceRealImpl(name)
            return@trace service.callMethod(method, *params)
        }
    }

    /**
     * 获取服务代理对象
     * @param name 服务名字
     * @return 服务代理
     */
    fun <T> getService(name: String): T {
        val proxy = nameToServiceProxy[name] ?: throw ServiceProxyNotFoundException(name)
        return proxy as T
    }

    /**
     * 获取服务 JavaScript 实现
     * @param name 服务名字
     * @return 服务 JavaScript 对象
     */
    fun getServiceRealImpl(name: String): JSValue {
        val serviceProvider =
            nameToServiceWrapper[name]?.getBalanceWrapper() ?: throw ServiceNotRegisterException(name)
        return if (!serviceProvider.isSingleton) {
            createService(serviceProvider)
        } else {
            nameToSingletonService[serviceProvider.nameSingleton()] ?: createService(serviceProvider)
        }
    }

    /**
     * 获取服务注册的线程 ID
     * @param name 服务名字
     * @return 线程 ID
     */
    fun getServiceRegisterTid(name: String): Int {
        val serviceProvider =
            nameToServiceWrapper[name]?.getBalanceWrapper() ?: throw ServiceNotRegisterException(name)
        return serviceProvider.registerTid
    }

    /**
     * 创建服务对象
     * @param provider
     * @return 服务对象
     */
    private fun createService(provider: ServiceWrapper): JSValue {
        return trace("createService service name = ${provider.name}, isSingleton = ${provider.isSingleton}") {
            tsfnRegister.callSyncSafe(provider.registerTid) {
                synchronized(this) {
                    // double check
                    if (provider.isSingleton && nameToSingletonService.contains(provider.nameSingleton())) {
                        return@callSyncSafe nameToSingletonService[provider.nameSingleton()]
                    }
                    val jsValue = JSValue(
                        newInstance(
                            provider.env, getReferenceValue(provider.env, provider.constructorRef)
                        )
                    )
                    if (provider.isSingleton) {
                        nameToSingletonService[provider.nameSingleton()] = jsValue
                    }
                    return@callSyncSafe jsValue
                }
            } ?: throw ServiceFactoryFailException(provider.name)
        }
    }
}

data class ServiceWrapper(
    val env: napi_env?,
    val name: String,
    val isSingleton: Boolean,
    val constructorRef: napi_ref?,
    val registerTid: Int = get_tid()
)