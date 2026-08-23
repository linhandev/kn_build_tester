package com.tencent.tmm.knoi.sample

import com.tencent.tmm.knoi.annotation.ServiceConsumer
import com.tencent.tmm.knoi.annotation.ServiceProvider

@ServiceConsumer
interface TestServiceAInCommon {
    // 传入 Int 返回 Int
    fun methodWithIntReturnInt(a: Int): Int
}

interface ServiceInCommonApi {
    // 传入 Int 返回 Int
    fun methodWithIntReturnInt(): String
}

@ServiceProvider(bind = ServiceInCommonApi::class)
open class ServiceAInCommon : ServiceInCommonApi {

    // 传入 Int 返回 Int
    override fun methodWithIntReturnInt(): String {
        return "hello tecent vieo"
    }
}

@ServiceProvider(bind = TestServiceInCommonApi::class)
open class TestServiceBInCommon : TestServiceInCommonApi {

    // 传入 Int 返回 Int
    override fun methodWithIntReturnInt(a: Int): Int {
        return a + 2
    }
}

@ServiceProvider()
open class TestServiceCInCommon : TestServiceInCommonApi {

    // 传入 Int 返回 Int
    override fun methodWithIntReturnInt(a: Int): Int {
        return a + 1
    }
}


interface TestServiceInCommonApi {
    // 传入 Int 返回 Int
    fun methodWithIntReturnInt(a: Int): Int
    fun a(){

    }
}