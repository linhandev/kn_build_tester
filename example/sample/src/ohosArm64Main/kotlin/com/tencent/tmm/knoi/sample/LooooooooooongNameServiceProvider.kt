package com.tencent.tmm.knoi.sample

import com.tencent.tmm.knoi.annotation.ServiceProvider
import com.tencent.tmm.knoi.logger.info

@ServiceProvider(singleton = false)
open class LooooooooooongNameServiceProvider {
    // 无参数调用
    fun methodWithUnit() {
        info("TestServiceB methodWithUnit")
    }
}