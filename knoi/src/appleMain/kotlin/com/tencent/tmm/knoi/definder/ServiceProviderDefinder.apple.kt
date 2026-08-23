package com.tencent.tmm.knoi.definder

import com.tencent.tmm.knoi.exception.ServiceGetException

actual inline fun <reified T> notFindService(name: String): T {
    throw ServiceGetException("${T::class.simpleName} cannot be obtained through generics; please check if the following situations apply: 1. ${T::class.simpleName} is not bound through the bind parameter of @ServiceProvider. 2. There are multiple implementations of the interface, requiring the use of generics to obtain instances. ")
}