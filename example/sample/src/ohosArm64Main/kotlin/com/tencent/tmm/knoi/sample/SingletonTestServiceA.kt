package com.tencent.tmm.knoi.sample

import com.tencent.tmm.knoi.annotation.ServiceConsumer

@ServiceConsumer
interface SingletonTestServiceA {
    fun method1(str: String)

    fun method2(str: String): String?
}