package com.tencent.tmm.knoiapp

import com.tencent.tmm.knoi.annotation.ServiceProvider
import com.tencent.tmm.knoi.sample.TestServiceInCommonApi

@ServiceProvider()
open class TestServiceCInAndroid: TestServiceInCommonApi {

    // 传入 Int 返回 Int
    override fun methodWithIntReturnInt(a: Int): Int {
        return a + 1
    }
}