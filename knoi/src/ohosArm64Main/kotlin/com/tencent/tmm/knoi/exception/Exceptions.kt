package com.tencent.tmm.knoi.exception

import com.tencent.tmm.knoi.type.JSValue
import platform.ohos.napi_value

class JavaScriptException(val value: napi_value?) : RuntimeException() {

    public val jsValue = JSValue(value)
    override val message: String?
        get() {
            if (value == null) {
                return null
            }
            return jsValue["message"]?.toKString()
        }

    fun getJavaScriptTopStackTrace(): String? {
        return jsValue["topstack"]?.toKString()
    }

    fun getJavaScriptStackTrace(): Array<String>? {
        val stack = jsValue["stack"] ?: return null
        return stack.toKString()?.split("\n")?.toTypedArray()
    }
}