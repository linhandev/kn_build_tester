package com.tencent.tmm.knoi.register

import com.tencent.tmm.knoi.jsbind.JSFunction
import com.tencent.tmm.knoi.logger.debug
import com.tencent.tmm.knoi.metric.trace
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * JavaScript Function 注册表
 */
class JSFunctionRegister : SynchronizedObject() {
    private val nameToJSFunctionMap = mutableMapOf<String, JSFunction>()

    /**
     * 注册 JS Function
     */
    fun register(jsFunction: JSFunction) {
        synchronized(this) {
            nameToJSFunctionMap[jsFunction.name] = jsFunction
        }
        debug("register JSFunction $jsFunction.name ${if (nameToJSFunctionMap[jsFunction.name] != null) "success" else "fail"} .")
    }

    /**
     * 取消注册 JS Function
     */
    fun unregister(name: String) {
        synchronized(this) {
            nameToJSFunctionMap.remove(name)
        }
        debug("unregister JSFunction $name ${if (nameToJSFunctionMap[name] == null) "success" else "fail"} .")
    }

    /**
     * 获取 JS Function
     */
    fun getJSFunction(name: String): JSFunction? {
        return trace("getJSFunction $name") {
            synchronized(this) {
                nameToJSFunctionMap[name]
            }
        }
    }

}


