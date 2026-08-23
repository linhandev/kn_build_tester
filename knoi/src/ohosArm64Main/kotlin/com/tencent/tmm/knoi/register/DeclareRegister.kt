package com.tencent.tmm.knoi.register

import com.tencent.tmm.knoi.logger.debug
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlin.reflect.KClass

data class DeclareInfo(
    val name: String,
    val declare: () -> Any,
)

class DeclareRegister : SynchronizedObject() {
    private val nameToDeclare = mutableMapOf<String, DeclareInfo>()
    fun register(info: DeclareInfo) {
        synchronized(this) {
            nameToDeclare[info.name] = info
        }
        debug("register Function ${info.name} ${if (nameToDeclare[info.name] != null) "success" else "fail"} .")
    }

    fun getDeclare(name: String): DeclareInfo? {
        synchronized(this) {
            return nameToDeclare[name]
        }
    }
}


