package com.tencent.tmm.knoi.sample

import com.tencent.tmm.knoi.annotation.ServiceProvider
import com.tencent.tmm.knoi.logger.logProxy
import com.tencent.tmm.knoi.module.loadModuleToJSValueWithInfo
import com.tencent.tmm.knoi.type.JSValue
import platform.ohos.LOG_ERROR

@ServiceProvider
open class TestModuleLoad {
    fun testModule() {
        val value = loadModuleToJSValueWithInfo("@ohos.display", null)

        val id = value.callMethod<JSValue>("getDefaultDisplaySync")?.get("height")

        logProxy(LOG_ERROR, "screenID:" + (id?.toInt() ?: 0))
    }
}