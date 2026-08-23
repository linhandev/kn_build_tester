package com.tencent.tmm.knoi.sample

import com.tencent.tmm.knoi.annotation.Hidden
import com.tencent.tmm.knoi.annotation.ServiceProvider
import com.tencent.tmm.knoi.logger.info
import kotlin.reflect.KClass

@ServiceProvider(singleton = true)
open class Lib1SingletonTestServiceB {
    init {
        info("SingletonTestServiceB init.")
    }

    fun method1(str: String) {
        info("SingletonTestServiceB method1 ${str}")
    }

    fun method2(str: String): String {
        info("SingletonTestServiceB method2 ${str}")
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