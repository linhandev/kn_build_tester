package com.tencent.tmm.knoi.register


import com.tencent.tmm.knoi.getEnv
import com.tencent.tmm.knoi.logger.debug
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.staticCFunction
import platform.ohos.knoi.callThreadSafeFunction
import platform.ohos.knoi.createThreadSafeFunctionWithSync
import platform.ohos.knoi.get_pid
import platform.ohos.knoi.get_tid
import platform.ohos.napi_threadsafe_function

class ThreadSafeFunctionRegister : SynchronizedObject() {

    private val tidToThreadSafeFunctionMap = mutableMapOf<Int, napi_threadsafe_function?>()

    /**
     * 注册 Thread Safe Function
     */
    fun registerThreadSafeFunctionIfNeed() {
        val tid = get_tid()
        synchronized(this) {
            if (tidToThreadSafeFunctionMap.containsKey(tid)) {
                return
            } else {
                tidToThreadSafeFunctionMap[tid] =
                    createThreadSafeFunctionWithSync(getEnv(), "tsfn-worker")
                debug("register thread safe function success. tid = ${get_tid()}")
            }
        }
    }

    /**
     * 将 block 在 tid 的 JS 线程执行
     * @param tid 线程 ID
     * @param sync 是否同步调用
     * @param block 待执行的闭包
     * @return 返回值
     */
    fun callFunctionInOtherThread(
        tid: Int,
        sync: Boolean,
        block: () -> COpaquePointer?
    ): COpaquePointer? {
        val tsfn: napi_threadsafe_function = tidToThreadSafeFunctionMap[tid]
            ?: throw RuntimeException("thread safe function not register.")

        val ref = StableRef.create(block)
        return callThreadSafeFunction(
            tsfn, staticCFunction(::callbackInJSThread), ref.asCPointer(), sync, tid
        )
    }

    /**
     * 在 JS 线程在同步调用 闭包，并返回
     * @param tid JavaScript 线程 ID
     * @param block 待执行的闭包
     */
    inline fun <reified R : Any> callSyncSafe(
        tid: Int, crossinline block: () -> R?
    ): R? {
        if (get_tid() == tid) {
            return block.invoke()
        } else {
            val ptr = callFunctionInOtherThread(tid, true) {
                val result = block.invoke() ?: return@callFunctionInOtherThread null
                val ref = StableRef.create(result)
                ref.asCPointer()
            }
            val ref = ptr?.asStableRef<R>() ?: return null
            val result = ref.get()
            ref.dispose()
            return result
        }
    }

    /**
     * 在 tid 的 JS 线程异步调用 block
     * @param tid 线程 ID
     * @param block 带执行的闭包
     */
    fun callAsyncSafe(
        tid: Int, block: () -> Unit
    ) {
        if (get_tid() == tid) {
            return block.invoke()
        } else {
            callFunctionInOtherThread(tid, false) {
                block.invoke()
                return@callFunctionInOtherThread null
            }
        }
    }

}

fun callbackInJSThread(ptr: COpaquePointer?): COpaquePointer? {
    val dataRef = ptr?.asStableRef<() -> COpaquePointer?>() ?: return null
    val block = dataRef.get()
    dataRef.dispose()
    return block.invoke()
}
