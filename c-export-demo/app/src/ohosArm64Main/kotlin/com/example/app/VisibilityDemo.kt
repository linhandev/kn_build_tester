@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.example.app

// ═══════════════════════════════════════════════════════════════════
// Scenario 4: Visibility control — what gets exported vs filtered out
// ═══════════════════════════════════════════════════════════════════

// ✅ EXPORTED: public top-level function
fun publicTopLevel(): String = "I am visible from C"

// ❌ NOT EXPORTED: internal — invisible to C
internal fun internalTopLevel(): String = "I am NOT visible from C"

// ❌ NOT EXPORTED: private — invisible to C
private fun privateTopLevel(): String = "I am NOT visible from C"

// ✅ EXPORTED: public class with mixed visibility members
class PublicClass {
    fun publicMethod(): String = "public method — exported"

    val publicProperty: Int = 42

    // ❌ NOT EXPORTED: internal method in public class
    internal fun internalMethod(): String = "internal method — not exported"

    // ❌ NOT EXPORTED: private method
    private fun privateMethod(): String = "private method — not exported"

    // ❌ NOT EXPORTED: protected method (only accessible from subclasses)
    protected fun protectedMethod(): String = "protected method — not exported"
}

// ❌ NOT EXPORTED: internal class — entire class hidden from C
internal class InternalClass {
    fun method(): String = "This whole class is hidden"
}

// ❌ NOT EXPORTED: private class
private class PrivateClass {
    fun method(): String = "This whole class is hidden"
}

// ═══════════════════════════════════════════════════════════════════
// Scenario 4B: Language features that are NEVER exported to C
// ═══════════════════════════════════════════════════════════════════

// ❌ NOT EXPORTED: suspend functions are filtered out by the C export pipeline
suspend fun suspendFunction(): String = "suspend functions are never in C header"

// ❌ NOT EXPORTED: generic functions (type parameters not representable in C)
fun <T> genericIdentity(value: T): T = value

// ❌ NOT EXPORTED: functions with context receivers (not representable in C)
// context(c: Boolean)
// fun contextFunction(): Int = 0

// ❌ NOT EXPORTED: expect declarations (no body)
// expect fun expectFunction(): Int

// value class: unwrapped to underlying type when used as parameter/return
// The class itself is NOT exported as a struct, but functions using it work fine.
@JvmInline
value class UserId(val id: Int)

fun lookupUser(uid: UserId): String = "User #${uid.id}"
