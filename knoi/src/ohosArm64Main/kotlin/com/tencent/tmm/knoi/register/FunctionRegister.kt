package com.tencent.tmm.knoi.register

import com.tencent.tmm.knoi.logger.debug
import com.tencent.tmm.knoi.metric.trace
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.reflect.KClass

data class FunctionBindInfo(
    val name: String,
    val originFun: (args: Array<out Any?>) -> Any?,
    val returnType: KClass<out Any>,
    val paramsType: Array<out KClass<out Any>>
)

/**
 * Kotlin Function 注册表
 */
class FunctionRegister : SynchronizedObject() {
    private val nameToFunctionBindInfoMap = mutableMapOf<String, FunctionBindInfo>()

    /**
     * 注册 Kotlin Function
     */
    fun register(bindInfo: FunctionBindInfo) {
        synchronized(this) {
            nameToFunctionBindInfoMap[bindInfo.name] = bindInfo
        }
        debug("register Function ${bindInfo.name} ${if (nameToFunctionBindInfoMap[bindInfo.name] != null) "success" else "fail"} .")
    }

    /**
     * 取消注册 Kotlin 方法
     */
    fun unRegister(name: String) {
        synchronized(this) {
            nameToFunctionBindInfoMap.remove(name)
        }
        debug("unRegister Function $name ${if (nameToFunctionBindInfoMap[name] != null) "success" else "fail"} .")
    }

    /**
     * 获取 Kotlin 方法
     */
    fun getFunction(name: String): ((Array<out Any?>) -> Any?)? {
        return trace("getFunction $name") {
            synchronized(this) {
                nameToFunctionBindInfoMap[name]?.originFun
            }
        }
    }

    /**
     * 获取 Kotlin 方法参数类型列表
     */
    fun getParamTypes(name: String): Array<out KClass<out Any>>? {
        synchronized(this) {
            return nameToFunctionBindInfoMap[name]?.paramsType
        }
    }

    /**
     * 获取 Kotlin 方法的返回值类型
     */
    fun getReturnType(name: String): KClass<out Any>? {
        synchronized(this) {
            return nameToFunctionBindInfoMap[name]?.returnType
        }
    }

}


