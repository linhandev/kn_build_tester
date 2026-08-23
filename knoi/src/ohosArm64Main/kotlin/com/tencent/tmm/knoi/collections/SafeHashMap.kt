package com.tencent.tmm.knoi.collections

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

class SafeHashMap<K, V> : SynchronizedObject() {

    private val map = mutableMapOf<K, V>()
    fun put(key: K, value: V) {
        synchronized(this) {
            map.put(key, value)
        }
    }

    fun get(key: K): V? {
        synchronized(this) {
            return map[key]
        }
    }

    fun remove(key: K): V? {
        synchronized(this) {
            return map.remove(key)
        }
    }

    fun size(): Int {
        synchronized(this) {
            return map.size
        }
    }

    fun forEach(action: (Map.Entry<K, V>) -> Unit) {
        synchronized(this) {
            map.forEach(action)
        }
    }

    fun removeByValueOrNull(value: V): V? {
        return synchronized(this) {
            val key = map.entries.firstOrNull { it.value == value }?.key
            map.remove(key)
        }
    }

    fun containsValue(value: V): Boolean {
        return synchronized(this) {
            map.containsValue(value)
        }
    }
}