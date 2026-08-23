package com.tencent.tmm.knoi.definder

import com.tencent.tmm.knoi.logger.debug
import com.tencent.tmm.knoi.register.ServiceProvider
import com.tencent.tmm.knoi.register.ServiceProviderRegister
import com.tencent.tmm.knoi.service.Invokable
import kotlin.reflect.KClass


val serviceProviderRegister = ServiceProviderRegister()


/**
 *  获取服务实现，优先同层服务
 *  @param name 服务名字，可选，不传则只发现 KMM 服务
 */
inline fun <reified T> getService(name: String = ""): T {
    // 优先同 Runtime 服务，减少跨 Runtime 调用损耗
    val service = if (name.isEmpty()) {
        serviceProviderRegister.getService<T>()
    } else {
        serviceProviderRegister.getServiceByName(name)
    }
    return if (service != null) {
        debug("getService ${T::class.simpleName} $name in ServiceProvider KMM.")
        service as T
    } else {
        debug("getService ${T::class.simpleName} $name in serviceProxy ArkTS.")
        notFindService<T>(name)
    }
}

expect inline fun <reified T> notFindService(name: String = ""): T


fun registerServiceProvider(
    name: String,
    api: KClass<out Any>,
    singleton: Boolean,
    factory: () -> Invokable
) {
    serviceProviderRegister.registerServiceProvider(ServiceProvider(name, api, singleton, factory))
}

fun registerServiceProvider(name: String, singleton: Boolean, factory: () -> Invokable) {
    serviceProviderRegister.registerServiceProvider(ServiceProvider(name, singleton, factory))
}