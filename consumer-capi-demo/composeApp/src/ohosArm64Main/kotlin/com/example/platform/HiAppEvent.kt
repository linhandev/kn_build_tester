@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.experimental.ExperimentalNativeApi::class,
)

package com.example.platform.HiAppEvent

import cnames.structs.HiAppEvent_Watcher
import cnames.structs.ParamListNode
import com.example.test.common.ApiGuard
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName
import kotlinx.cinterop.*
import platform.PerformanceAnalysisKit.HiAppEvent.DOMAIN_OS
import platform.PerformanceAnalysisKit.HiAppEvent.EVENT_APP_CRASH
import platform.PerformanceAnalysisKit.HiAppEvent.HiAppEvent_AppEventGroup
import platform.PerformanceAnalysisKit.HiAppEvent.OH_HiAppEvent_AddFloatParam
import platform.PerformanceAnalysisKit.HiAppEvent.OH_HiAppEvent_AddInt16Param
import platform.PerformanceAnalysisKit.HiAppEvent.OH_HiAppEvent_AddInt8Param
import platform.PerformanceAnalysisKit.HiAppEvent.OH_HiAppEvent_AddWatcher
import platform.PerformanceAnalysisKit.HiAppEvent.OH_HiAppEvent_CreateParamList
import platform.PerformanceAnalysisKit.HiAppEvent.OH_HiAppEvent_CreateWatcher
import platform.PerformanceAnalysisKit.HiAppEvent.OH_HiAppEvent_DestroyParamList
import platform.PerformanceAnalysisKit.HiAppEvent.OH_HiAppEvent_DestroyWatcher
import platform.PerformanceAnalysisKit.HiAppEvent.OH_HiAppEvent_RemoveWatcher
import platform.PerformanceAnalysisKit.HiAppEvent.OH_HiAppEvent_SetAppEventFilter
import platform.PerformanceAnalysisKit.HiAppEvent.OH_HiAppEvent_SetWatcherOnReceive
import platform.PerformanceAnalysisKit.HiAppEvent.OH_HiAppEvent_Write
import platform.PerformanceAnalysisKit.HiAppEvent.PARAM_USER_ID
import platform.posix.strdup
import platform.posix.usleep

private val hiAppEventCallbackLines = mutableListOf<String>()

private fun hiAppEventCbAppend(line: String) {
    hiAppEventCallbackLines.add(line)
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
private val hiAppEventOnReceive =
    staticCFunction { domain: CPointer<ByteVar>?, _: CPointer<HiAppEvent_AppEventGroup>?, groupLen: UInt ->
        val d = domain?.toKString().orEmpty().ifBlank { "(空)" }
        hiAppEventCbAppend(
            "[回调→页面] HiAppEvent Watcher：OH_HiAppEvent_OnReceive 已触发 domain=\"$d\" groupLen=$groupLen",
        )
    }

private enum class SmokeOut { Ok, Fail, Ver }

/** 场景一清单 6 项（与需求图一致）。 */
private val HIAPPEVENT_CHECKLIST_6 = listOf(
    "OH_HiAppEvent_CreateParamList",
    "OH_HiAppEvent_AddInt8Param",
    "OH_HiAppEvent_AddInt16Param",
    "OH_HiAppEvent_AddFloatParam",
    "OH_HiAppEvent_Write",
    "OH_HiAppEvent_DestroyParamList",
)

private val HIAPPEVENT_CHECKLIST_SET = HIAPPEVENT_CHECKLIST_6.toSet()

private fun hiAppStatKeyFromLabel(label: String): String? =
    label.substringBefore("(").trim().takeIf { it in HIAPPEVENT_CHECKLIST_SET }

@Suppress("MagicNumber", "LongMethod", "CognitiveComplexity")
fun buildHiAppEventModuleSmokeReport(
    @Suppress("UNUSED_PARAMETER") databaseDir: String,
    bundleName: String,
    moduleName: String,
): String {
    val failDetails = mutableListOf<String>()
    val verDetails = mutableListOf<String>()
    val macroMismatchDetails = mutableListOf<String>()
    val checklistOutcome = mutableMapOf<String, SmokeOut>()

    fun mergeChecklist(key: String, o: SmokeOut) {
        require(key in HIAPPEVENT_CHECKLIST_SET) { "unknown checklist key: $key" }
        fun rank(x: SmokeOut) = when (x) {
            SmokeOut.Ok -> 0
            SmokeOut.Fail -> 1
            SmokeOut.Ver -> 2
        }
        val prev = checklistOutcome[key]
        checklistOutcome[key] = if (prev == null) {
            o
        } else {
            if (rank(o) >= rank(prev)) o else prev
        }
    }

    data class MacroCheck(val name: String, val fixed: String, val cinterop: String) {
        val ok: Boolean get() = fixed == cinterop
    }

    /** 与固定字符串 `"OS"`、`"APP_CRASH"`、`"user_id"` 比对 cinterop 包内宏常量。 */
    val macroChecks = listOf(
        MacroCheck("DOMAIN_OS", "OS", DOMAIN_OS),
        MacroCheck("EVENT_APP_CRASH", "APP_CRASH", EVENT_APP_CRASH),
        MacroCheck("PARAM_USER_ID", "user_id", PARAM_USER_ID),
    )

    return buildString {
        appendLine("=== HiAppEvent 用户打点 CAPI 验证（platform.PerformanceAnalysisKit.HiAppEvent.*）===")
        appendLine("bundle=$bundleName module=$moduleName")
        appendLine(
            "场景一：CreateParamList → AddInt8/AddInt16/AddFloat（真实数值与 PARAM_USER_ID 等宏作参名）→ " +
                "Write(DOMAIN_OS,EVENT_APP_CRASH,FAULT,list) → DestroyParamList。" +
                "Watcher(OnReceive) 用于验证回调到达；清单统计仍只计上述 6 个 API。",
        )
        appendLine(
            "文档：[capi-hiappevent-h](https://developer.huawei.com/consumer/cn/doc/harmonyos-references/capi-hiappevent-h)。",
        )
        appendLine()

        appendLine("---------- 宏定义比对（cinterop 取值 vs 固定参照 \"OS\" / \"APP_CRASH\" / \"user_id\"）----------")
        var macroOk = 0
        var macroFail = 0
        macroChecks.forEachIndexed { i, m ->
            if (m.ok) {
                macroOk++
                appendLine("${i + 1}. ${m.name} — 一致：cinterop=\"${m.cinterop}\"（参照=\"${m.fixed}\"）")
            } else {
                macroFail++
                val line =
                    "• ${m.name} — 不一致：参照=\"${m.fixed}\" cinterop=\"${m.cinterop}\""
                macroMismatchDetails.add(line)
                appendLine("${i + 1}. ${m.name} — 不一致：参照=\"${m.fixed}\" cinterop=\"${m.cinterop}\"")
            }
        }
        appendLine()
        appendLine("宏比对汇总：一致=$macroOk 不一致=$macroFail")
        appendLine()

        hiAppEventCallbackLines.clear()

        val body = memScoped {
            val sb = StringBuilder()

            fun record(
                name: String,
                raw: String,
                o: SmokeOut,
                extra: String? = null,
                statKey: String? = null,
                omitFailDetail: Boolean = false,
            ) {
                val key = statKey ?: hiAppStatKeyFromLabel(name)
                if (key != null) {
                    mergeChecklist(key, o)
                }
                sb.appendLine("$name: $raw")
                sb.appendLine("  → ${extra ?: ""}")
                when (o) {
                    SmokeOut.Fail ->
                        if (!omitFailDetail) {
                            failDetails.add("• $name：结果 $raw。${extra?.trim().orEmpty()}")
                        }
                    SmokeOut.Ver ->
                        verDetails.add("• $name：结果 $raw。${extra?.trim().orEmpty()}")
                    SmokeOut.Ok -> Unit
                }
            }

            fun zhHiAppWrite(code: Int): String = when {
                code == ApiGuard.CODE_API_VERSION_TOO_HIGH ->
                    "释义：API_VERSION_TOO_HIGH（ApiGuard）。"
                code == 0 ->
                    "释义：0 — 参数校验成功并已写入事件文件。"
                code > 0 ->
                    "释义：正数 — 存在无效参数已忽略后仍写入（见官方码表）。"
                code == -1 ->
                    "释义：-1 — 事件名无效。"
                code == -4 ->
                    "释义：-4 — 事件 domain 无效。"
                code == -99 ->
                    "释义：-99 — 功能关闭。"
                else ->
                    "释义：负数 — 校验失败未写入；见 hiappevent.h OH_HiAppEvent_Write。"
            }

            var plist: CPointer<ParamListNode>? = OH_HiAppEvent_CreateParamList()
            val createOk = plist != null
            record(
                "OH_HiAppEvent_CreateParamList()",
                if (createOk) "non-null ParamList" else "null",
                if (createOk) SmokeOut.Ok else SmokeOut.Fail,
                if (createOk) {
                    "释义：ParamList 节点指针非空，后续 Add* 与 Write 使用该指针（object-like / to_pointer 语义）。"
                } else {
                    "释义：返回 null，无法继续。"
                },
                "OH_HiAppEvent_CreateParamList",
            )

            if (plist != null) {
                plist = OH_HiAppEvent_AddInt8Param(plist, PARAM_USER_ID, (-42).toByte())
                val ok8 = plist != null
                record(
                    "OH_HiAppEvent_AddInt8Param(list,name=PARAM_USER_ID(\"$PARAM_USER_ID\"),num=-42)",
                    if (ok8) "non-null" else "null",
                    if (ok8) SmokeOut.Ok else SmokeOut.Fail,
                    "释义：param_primitive_schar（int8_t）；参名使用预设宏 PARAM_USER_ID。",
                    "OH_HiAppEvent_AddInt8Param",
                )

                plist = OH_HiAppEvent_AddInt16Param(plist, "kmp_sample_i16", 32000)
                val ok16 = plist != null
                record(
                    "OH_HiAppEvent_AddInt16Param(list,name=\"kmp_sample_i16\",num=32000)",
                    if (ok16) "non-null" else "null",
                    if (ok16) SmokeOut.Ok else SmokeOut.Fail,
                    "释义：param_primitive_short（int16_t）。",
                    "OH_HiAppEvent_AddInt16Param",
                )

                plist = OH_HiAppEvent_AddFloatParam(plist, "kmp_sample_float", 2.718281828f)
                val okf = plist != null
                record(
                    "OH_HiAppEvent_AddFloatParam(list,name=\"kmp_sample_float\",num=2.718281828f)",
                    if (okf) "non-null" else "null",
                    if (okf) SmokeOut.Ok else SmokeOut.Fail,
                    "释义：param_primitive_float。",
                    "OH_HiAppEvent_AddFloatParam",
                )

                val watcher: CPointer<HiAppEvent_Watcher>? =
                    OH_HiAppEvent_CreateWatcher("kmp_hiappevent_smoke_watcher")
                sb.appendLine(
                    "(Watcher 辅助) OH_HiAppEvent_CreateWatcher(name=\"kmp_hiappevent_smoke_watcher\"): " +
                        "${if (watcher != null) "non-null" else "null"}",
                )
                run {
                    val w = watcher ?: return@run
                    val recvRc = OH_HiAppEvent_SetWatcherOnReceive(w, hiAppEventOnReceive)
                    sb.appendLine("  → SetWatcherOnReceive(OnReceive=staticCFunction)：$recvRc（0 为成功）")
                    val crashNamePtr = EVENT_APP_CRASH.cstr.getPointer(this@memScoped)
                    val namesArr = allocArrayOf(crashNamePtr)
                    val filterRc = OH_HiAppEvent_SetAppEventFilter(
                        w,
                        DOMAIN_OS,
                        0x01u.toUByte(),
                        namesArr,
                        1,
                    )
                    sb.appendLine(
                        "  → SetAppEventFilter(domain=DOMAIN_OS,eventTypes=0x01 FAULT,names=[EVENT_APP_CRASH])：$filterRc",
                    )
                    val addWRc = OH_HiAppEvent_AddWatcher(w)
                    sb.appendLine("  → OH_HiAppEvent_AddWatcher：$addWRc")
                }

                // 头文件 enum EventType { FAULT = 1, ... }；绑定侧第三参为数值类型时传 1。
                val writeRcRaw = ApiGuard.guardInt {
                    OH_HiAppEvent_Write(DOMAIN_OS, EVENT_APP_CRASH, 1u, plist!!)
                }
                val verHint = ApiGuard.takeVersionHint()
                val rawW = if (writeRcRaw == ApiGuard.CODE_API_VERSION_TOO_HIGH) "API_VERSION_TOO_HIGH" else "$writeRcRaw"
                val oW = when {
                    writeRcRaw == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                    writeRcRaw == 0 || writeRcRaw > 0 -> SmokeOut.Ok
                    else -> SmokeOut.Fail
                }
                val extraW = buildString {
                    append(zhHiAppWrite(writeRcRaw))
                    append(" domain=DOMAIN_OS name=EVENT_APP_CRASH type=FAULT。")
                    if (!verHint.isNullOrBlank()) {
                        append(" ")
                        append(verHint)
                    }
                }
                record(
                    "OH_HiAppEvent_Write(domain=DOMAIN_OS,name=EVENT_APP_CRASH,type=FAULT,list=ParamList)",
                    rawW,
                    oW,
                    extraW,
                    "OH_HiAppEvent_Write",
                )

                usleep(300_000u)
                sb.appendLine("---------- Watcher OnReceive 采样（Write 后延迟 300ms）----------")
                if (hiAppEventCallbackLines.isEmpty()) {
                    sb.appendLine("（无 [回调→页面] 输出：可能未投递到当前 watcher 或事件被策略拒绝）")
                } else {
                    hiAppEventCallbackLines.forEach { sb.appendLine(it) }
                }
                val hasProbe = hiAppEventCallbackLines.any { it.startsWith("[回调→页面]") }
                sb.appendLine(
                    "[回调验证] HiAppEvent Watcher OnReceive：${if (hasProbe) "成功" else "未确认"}",
                )
                sb.appendLine(
                    "  → 释义：${if (hasProbe) {
                        "已出现 [回调→页面] 行，staticCFunction 已接到 native。"
                    } else {
                        "时限内无探针；部分系统事件组合可能不触发应用 watcher。"
                    }}",
                )
                if (!hasProbe && watcher != null) {
                    failDetails.add("• [回调验证]：AddWatcher 已执行但未采集到 OnReceive 探针。")
                }
                sb.appendLine()

                OH_HiAppEvent_DestroyParamList(plist!!)
                record(
                    "OH_HiAppEvent_DestroyParamList(list)",
                    "void",
                    SmokeOut.Ok,
                    "释义：释放 ParamList 链表节点。",
                    "OH_HiAppEvent_DestroyParamList",
                )

                watcher?.let { w ->
                    OH_HiAppEvent_RemoveWatcher(w)
                    OH_HiAppEvent_DestroyWatcher(w)
                    sb.appendLine("(Watcher 辅助) RemoveWatcher + DestroyWatcher 已调用")
                }
            }

            sb.toString()
        }

        append(body)

        appendLine()
        appendLine("---------- 清单统计（共 ${HIAPPEVENT_CHECKLIST_6.size} 项）----------")
        var ckOk = 0
        var ckFail = 0
        var ckVer = 0
        var ckNotRun = 0
        HIAPPEVENT_CHECKLIST_6.forEachIndexed { index, key ->
            val o = checklistOutcome[key]
            val label = when (o) {
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
        macroChecks.forEachIndexed { i, m ->
            val tag = if (m.ok) "一致" else "不一致"
            appendLine(
                "${i + 1}. ${m.name} — 固定参照 \"${m.fixed}\" — cinterop \"${m.cinterop}\" — $tag",
            )
        }
        appendLine()
        appendLine("宏列表汇总：一致=${macroChecks.count { it.ok }} 不一致=${macroChecks.count { !it.ok }}")
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
        val notRunKeys = HIAPPEVENT_CHECKLIST_6.filter { checklistOutcome[it] == null }
        if (notRunKeys.isEmpty()) {
            appendLine("（无）")
        } else {
            notRunKeys.forEach { k ->
                appendLine("• $k — 前置失败或未合并 record。")
            }
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

@CName("kn_runHiAppEventModuleSmokeTest")
fun kn_runHiAppEventModuleSmokeTest(
    dbDir: CPointer<ByteVar>?,
    bundleName: CPointer<ByteVar>?,
    moduleName: CPointer<ByteVar>?,
): CPointer<ByteVar>? {
    val dir = dbDir?.toKString().orEmpty()
    val bundle = bundleName?.toKString().orEmpty()
    val module = moduleName?.toKString().orEmpty()
    val report = try {
        buildHiAppEventModuleSmokeReport(dir, bundle, module)
    } catch (t: Throwable) {
        "Kotlin 异常: ${t::class.simpleName}: ${t.message}\n${t.stackTraceToString()}"
    }
    return strdup(report)
}
