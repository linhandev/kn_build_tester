package com.tencent.tmm.knoi.type


typealias JSCallbackArgs = Array<JSValue>
typealias JSCallback = (JSCallbackArgs) -> Any?

fun JSValue.asPromise(): Promise {
    return Promise(this)
}

class Promise(val jsValue: JSValue) {
    fun then(callBack: JSCallback): Promise {
        jsValue.callMethod<Unit>("then", callBack)
        return this
    }

    fun catch(callBack: JSCallback): Promise {
        jsValue.callMethod<Unit>("catch", callBack)
        return this
    }

}