package com.example.test.common

import kotlin.collections.ArrayDeque

/**
 * 包装可能因 **API/能力版本与编译期声明不匹配** 而抛出 [IllegalStateException] 的 native 调用。
 * 捕获后返回占位值，并通过 [takeVersionHint] 取出面向用户的说明（供报告/界面展示）。
 *
 * **推荐**：每个 native 调用只包 **一层** —— 直接 `guard { … }` / `guardInt { … }` / `guardInvoke { … }`，
 * 或在 RDB 报告里用 `gInt("OH_…") { OH_…() }`（其内部已 [guardInt]）。
 * 请勿在 `gInt { }` 内再套一层 `guardInt { }`，属于重复包装。
 */
object ApiGuard {
    const val CODE_API_VERSION_TOO_HIGH = -10001
    const val STR_API_VERSION_TOO_HIGH = "API_VERSION_TOO_HIGH"

    private val versionHintStack = ArrayDeque<String>()

    /** 供报告拼接：取最近一次版本相关拦截说明（LIFO），无则返回 null。 */
    fun takeVersionHint(): String? = versionHintStack.removeLastOrNull()

    private fun pushVersionHint(message: String) {
        versionHintStack.addLast(message)
    }

    /**
     * 从 [IllegalStateException] 解析可读提示；若无法解析具体版本号则使用兜底文案。
     */
    fun describeApiVersionMismatch(e: IllegalStateException): String {
        val msg = e.message?.trim().orEmpty()
        // 常见：含数字的版本片段
        val versionRegex = Regex("""(?i)(?:api|sdk|version|版本|Level)[^\d]{0,12}(\d+(?:\.\d+)*)""")
        val ver = versionRegex.find(msg)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
        return if (!ver.isNullOrBlank()) {
            "当前 API 版本 $ver 过低或与编译声明不匹配，不支持调用该函数。原始异常：$msg"
        } else if (msg.isNotEmpty()) {
            "当前 API 版本过低或与编译声明不匹配，不支持调用该函数。详情：$msg"
        } else {
            "当前 API 版本过低或与编译声明不匹配，不支持调用该函数（无法从异常中解析具体版本号）。"
        }
    }

    fun <T> guard(block: () -> T, illegalStateValue: T, otherErrorValue: T): T {
        return try {
            block()
        } catch (e: IllegalStateException) {
            pushVersionHint(describeApiVersionMismatch(e))
            illegalStateValue
        } catch (_: Throwable) {
            otherErrorValue
        }
    }

    fun guardInt(block: () -> Int): Int =
        guard(block, CODE_API_VERSION_TOO_HIGH, -1)

    fun guardLong(block: () -> Long): Long =
        guard(block, CODE_API_VERSION_TOO_HIGH.toLong(), -1L)

    fun guardDouble(block: () -> Double): Double =
        guard(block, CODE_API_VERSION_TOO_HIGH.toDouble(), -1.0)

    fun guardString(block: () -> String): String =
        guard(block, STR_API_VERSION_TOO_HIGH, "error")

    fun guardBoolean(block: () -> Boolean): Boolean =
        guard(block, false, false)

    /**
     * 无返回值（或仅关心是否抛 [IllegalStateException]）的 native 调用，成功返回 `0`，
     * 版本不匹配返回 [CODE_API_VERSION_TOO_HIGH]，其它异常返回 `-1`。
     */
    fun guardInvoke(block: () -> Unit): Int =
        try {
            block()
            0
        } catch (e: IllegalStateException) {
            pushVersionHint(describeApiVersionMismatch(e))
            CODE_API_VERSION_TOO_HIGH
        } catch (_: Throwable) {
            -1
        }
}
