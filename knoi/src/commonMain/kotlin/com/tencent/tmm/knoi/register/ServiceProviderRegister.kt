package com.tencent.tmm.knoi.register

import com.tencent.tmm.knoi.exception.ServiceRegisterException
import com.tencent.tmm.knoi.logger.debug
import com.tencent.tmm.knoi.service.Invokable
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.reflect.KClass

class ServiceProviderRegister : SynchronizedObject() {
    val apiToProvider = mutableMapOf<KClass<out Any>, ServiceProvider>()
    private val nameToSingleInvokable = mutableMapOf<String, Invokable>()
    private val nameToProvider = mutableMapOf<String, ServiceProvider>()
    val hashToServiceProxy = mutableMapOf<String, Invokable>()

    fun registerServiceProvider(provider: ServiceProvider) {
        synchronized(this) {
            nameToProvider[provider.name] = provider
            if (provider.api != null) {
                if (apiToProvider[provider.api] != null) {
                    throw ServiceRegisterException("The multiple implementation of interfaces(${provider.api.simpleName}) cannot declare bind parameters in the @ServiceProvider; please differentiate them by using names.")
                }
                apiToProvider[provider.api] = provider
            }
        }
        debug("register Service $provider.name ${if (nameToProvider[provider.name] != null) "success" else "fail"} .")
    }

    inline fun <reified T> getService(): Invokable? {
        synchronized(this) {
            val serviceProvider =
                apiToProvider[T::class] ?: return null
            return getServiceByName(serviceProvider.name)
        }
    }

    fun releaseInvokable(invokable: Invokable) {
        hashToServiceProxy.remove(invokable.hashCode().toString())
    }

    fun getServiceByName(name: String): Invokable? {
        val serviceProvider =
            nameToProvider[name] ?: run {
                debug("getServiceByName ServiceNotRegisterException name = $name")
                return null
            }
        val service = if (!serviceProvider.singleton || nameToSingleInvokable[name] == null) {
            serviceProvider.factory.invoke().also {
                if (serviceProvider.singleton) {
                    nameToSingleInvokable[name] = it
                }
            }
        } else {
            nameToSingleInvokable[name]
        }
        if (service == null) {
            debug("getServiceByName ServiceFactoryFailException name = $name")
            return null
        }
        return service
    }


}

data class ServiceProvider(
    val name: String,
    val api: KClass<out Any>? = null,
    val singleton: Boolean,
    val factory: () -> Invokable
) {
    constructor(name: String, singleton: Boolean, factory: () -> Invokable) : this(
        name,
        null,
        singleton,
        factory
    )
}