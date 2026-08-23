package com.tencent.tmm.knoi.sample

import com.tencent.tmm.knoi.annotation.Hidden
import com.tencent.tmm.knoi.annotation.ServiceProvider
import com.tencent.tmm.knoi.logger.info
import kotlin.reflect.KClass

@ServiceProvider
open class Lib1TestServiceB {
    init {
        info("TestServiceB init.")
    }

    fun method1(str: String) {
        info("TestServiceB method1 ${str}")
    }

    fun method2(str: String): String {
        info("TestServiceB method2 ${str}")
        return str + "InKMM"
    }


    @Hidden
    fun method3(str: KClass<Any>): String {
        return "Hidden"
    }

    private fun method4(str: KClass<Any>): String {
        return "Hidden"
    }
}