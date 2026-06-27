@file:OptIn(
        kotlinx.cinterop.ExperimentalForeignApi::class,
        kotlin.experimental.ExperimentalNativeApi::class,
)

package com.example.platform.HiLog

import com.example.test.common.ApiGuard
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.PerformanceAnalysisKit.HiLog.LOG_APP
import platform.PerformanceAnalysisKit.HiLog.LOG_DEBUG
import platform.PerformanceAnalysisKit.HiLog.LOG_DOMAIN
import platform.PerformanceAnalysisKit.HiLog.LOG_INFO
import platform.PerformanceAnalysisKit.HiLog.LOG_TAG
import platform.PerformanceAnalysisKit.HiLog.OH_LOG_Print
import platform.posix.strdup

private enum class SmokeOut {
    Ok,
    Fail,
    Ver
}

/** 与 log.h 中可调符号一致：OH_LOG_Print 带 `...`；*/
private val HILOG_CHECKLIST = listOf(
    "OH_LOG_Print",
)

private val HILOG_CHECKLIST_SET = HILOG_CHECKLIST.toSet()

private fun hiLogStatKeyFromLabel(label: String): String? =
        label.substringBefore("(").trim().takeIf { it in HILOG_CHECKLIST_SET }

@Suppress("MagicNumber", "LongMethod", "CognitiveComplexity")
fun buildHiLogModuleSmokeReport(
        @Suppress("UNUSED_PARAMETER") databaseDir: String,
        bundleName: String,
        moduleName: String,
): String {
    val failDetails = mutableListOf<String>()
    val verDetails = mutableListOf<String>()
    val macroMismatchDetails = mutableListOf<String>()
    val checklistOutcome = mutableMapOf<String, SmokeOut>()

    fun mergeChecklist(key: String, o: SmokeOut) {
        require(key in HILOG_CHECKLIST_SET) { "unknown checklist key: $key" }
        fun rank(x: SmokeOut) =
                when (x) {
                    SmokeOut.Ok -> 0
                    SmokeOut.Fail -> 1
                    SmokeOut.Ver -> 2
                }
        val prev = checklistOutcome[key]
        checklistOutcome[key] =
                if (prev == null) {
                    o
                } else {
                    if (rank(o) >= rank(prev)) o else prev
                }
    }

    /** 固定参照：LOG_DOMAIN=0；LOG_TAG=NULL（cinterop 常将 NULL 标量化为 0L）。 */
    val domainOk = LOG_DOMAIN.toUInt() == 0u
    val tagOk = LOG_TAG == 0L
    val macroLines =
            listOf(
                    Triple(
                            "LOG_DOMAIN",
                            "固定参照 0",
                            if (domainOk) "cinterop=$LOG_DOMAIN — 一致"
                            else "cinterop=$LOG_DOMAIN — 不一致",
                    ),
                    Triple(
                            "LOG_TAG",
                            "固定参照 NULL",
                            if (tagOk) "cinterop=0L(NULL) — 一致" else "cinterop=$LOG_TAG — 不一致",
                    ),
            )
    if (!domainOk) {
        macroMismatchDetails.add("• LOG_DOMAIN — 不一致：参照=0 cinterop=$LOG_DOMAIN")
    }
    if (!tagOk) {
        macroMismatchDetails.add("• LOG_TAG — 不一致：参照=NULL")
    }

    return buildString {
        appendLine("=== HiLog 日志 CAPI 验证（platform.PerformanceAnalysisKit.HiLog.*）===")
        appendLine("bundle=$bundleName module=$moduleName")
        appendLine(
            "场景一（对照 `log.h`）：" +
                "① `OH_LOG_Print(..., const char *fmt, ...)`（约 181 行）— Kotlin 在 `fmt` 之后传入多个实参，对应头文件 `@param ...` 与格式串中的多个说明符；"
        )
        appendLine(
                "文档：[capi-log-h](https://developer.huawei.com/consumer/cn/doc/harmonyos-references/capi-log-h)。",
        )
        appendLine()

        appendLine("---------- 宏定义比对（cinterop vs 固定参照 0 / NULL）----------")
        macroLines.forEachIndexed { i, t ->
            appendLine("${i + 1}. ${t.first} — ${t.second} — ${t.third}")
        }
        appendLine()
        val macroOkCnt = (if (domainOk) 1 else 0) + (if (tagOk) 1 else 0)
        appendLine("宏比对汇总：一致=$macroOkCnt 不一致=${2 - macroOkCnt}")
        appendLine()

        val body = buildString {
            fun record(
                    name: String,
                    raw: String,
                    o: SmokeOut,
                    extra: String? = null,
                    statKey: String? = null,
            ) {
                val key = statKey ?: hiLogStatKeyFromLabel(name)
                if (key != null) {
                    mergeChecklist(key, o)
                }
                appendLine("$name: $raw")
                appendLine("  → ${extra ?: ""}")
                when (o) {
                    SmokeOut.Fail -> failDetails.add("• $name：结果 $raw。${extra?.trim().orEmpty()}")
                    SmokeOut.Ver -> verDetails.add("• $name：结果 $raw。${extra?.trim().orEmpty()}")
                    SmokeOut.Ok -> Unit
                }
            }

            fun zhLogRc(v: Int): String =
                    when {
                        v == ApiGuard.CODE_API_VERSION_TOO_HIGH ->
                                "释义：API_VERSION_TOO_HIGH（ApiGuard）。"
                        v >= 0 -> "释义：≥0 表示本次调用被 HiLog 接受。"
                        else -> "释义：<0 表示失败，见 capi-log-h。"
                    }

            fun gLogInt(name: String, statKey: String, block: () -> Int) {
                val v = ApiGuard.guardInt(block)
                val verHint = ApiGuard.takeVersionHint()
                val raw =
                        if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) "API_VERSION_TOO_HIGH"
                        else "$v"
                val o =
                        when {
                            v == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                            v >= 0 -> SmokeOut.Ok
                            else -> SmokeOut.Fail
                        }
                val extra = buildString {
                    append(zhLogRc(v))
                    if (!verHint.isNullOrBlank()) {
                        append(" ")
                        append(verHint)
                    }
                }
                record(name, raw, o, extra, statKey)
            }

            gLogInt(
                    "OH_LOG_Print(LOG_APP, LOG_INFO, 0u, \"tag\", \"a=%{public}d b=%{public}d\", 7, 42) — fmt 后可变实参 `...`",
                    "OH_LOG_Print",
            ) {
                OH_LOG_Print(
                        LOG_APP,
                        LOG_INFO,
                        0u,
                        "tag",
                        "a=%{public}d b=%{public}d",
                        7,
                        42,
                )
            }
        }

        append(body)

        appendLine()
        appendLine("---------- 清单统计（共 ${HILOG_CHECKLIST.size} 项）----------")
        var ckOk = 0
        var ckFail = 0
        var ckVer = 0
        var ckNotRun = 0
        HILOG_CHECKLIST.forEachIndexed { index, key ->
            val o = checklistOutcome[key]
            val label =
                    when (o) {
                        null -> {
                            ckNotRun++
                            "未执行"
                        }
                        SmokeOut.Ok -> {
                            ckOk++
                            "成功"
                        }
                        SmokeOut.Fail -> {
                            ckFail++
                            "失败"
                        }
                        SmokeOut.Ver -> {
                            ckVer++
                            "API版本不符"
                        }
                    }
            appendLine("${index + 1}. $key — $label")
        }
        appendLine()
        appendLine("清单汇总：成功=$ckOk  失败=$ckFail  API版本不符=$ckVer  未执行=$ckNotRun")
        appendLine()
        appendLine("---------- 宏定义列表 ----------")
        macroLines.forEachIndexed { i, t ->
            val ok = (i == 0 && domainOk) || (i == 1 && tagOk)
            appendLine(
                    "${i + 1}. ${t.first} — ${t.second} — ${t.third} — ${if (ok) "一致" else "不一致"}"
            )
        }
        appendLine()
        appendLine("宏列表汇总：一致=$macroOkCnt 不一致=${2 - macroOkCnt}")
        appendLine()
        appendLine("---------- 判定失败明细 ----------")
        if (failDetails.isEmpty()) {
            appendLine("（无）")
        } else {
            failDetails.forEach { appendLine(it) }
        }
        appendLine()
        appendLine("---------- API 版本不符（ApiGuard）明细 ----------")
        if (verDetails.isEmpty()) {
            appendLine("（无）")
        } else {
            verDetails.forEach { appendLine(it) }
        }
        appendLine()
        appendLine("---------- 未执行明细 ----------")
        val notRunKeys = HILOG_CHECKLIST.filter { checklistOutcome[it] == null }
        if (notRunKeys.isEmpty()) {
            appendLine("（无）")
        } else {
            notRunKeys.forEach { k -> appendLine("• $k — 前置失败或未合并 record。") }
        }
        appendLine()
        appendLine("---------- 宏定义比对失败明细 ----------")
        if (macroMismatchDetails.isEmpty()) {
            appendLine("（无）")
        } else {
            macroMismatchDetails.forEach { appendLine(it) }
        }
    }
}

@CName("kn_runHiLogModuleSmokeTest")
fun kn_runHiLogModuleSmokeTest(
        dbDir: CPointer<ByteVar>?,
        bundleName: CPointer<ByteVar>?,
        moduleName: CPointer<ByteVar>?,
): CPointer<ByteVar>? {
    val dir = dbDir?.toKString().orEmpty()
    val bundle = bundleName?.toKString().orEmpty()
    val module = moduleName?.toKString().orEmpty()
    val report =
            try {
                buildHiLogModuleSmokeReport(dir, bundle, module)
            } catch (t: Throwable) {
                "Kotlin 异常: ${t::class.simpleName}: ${t.message}\n${t.stackTraceToString()}"
            }
    return strdup(report)
}
