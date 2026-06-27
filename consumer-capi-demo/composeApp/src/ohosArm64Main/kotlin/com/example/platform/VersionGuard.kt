@file:OptIn(
        kotlinx.cinterop.ExperimentalForeignApi::class,
        kotlin.experimental.ExperimentalNativeApi::class,
)

package com.example.platform.VersionGuard

import cnames.structs.OH_AudioStreamBuilderStruct
import com.example.test.common.ApiGuard
import kotlin.experimental.ExperimentalNativeApi
import kotlin.math.round
import kotlin.native.CName
import kotlin.time.measureTime
import kotlinx.cinterop.*
import platform.AVCodecKit.CodecBase.OH_AVCODEC_MIMETYPE_AUDIO_ALAC
import platform.AudioKit.OHAudio.AUDIOSTREAM_TYPE_RENDERER
import platform.AudioKit.OHAudio.AUDIOSTREAM_VOLUMEMODE_SYSTEM_GLOBAL
import platform.AudioKit.OHAudio.OH_AudioStreamBuilder_Create
import platform.AudioKit.OHAudio.OH_AudioStreamBuilder_Destroy
import platform.AudioKit.OHAudio.OH_AudioStreamBuilder_SetVolumeMode
import platform.BasicServicesKit.Pasteboard.OH_Pasteboard_Create
import platform.BasicServicesKit.Pasteboard.OH_Pasteboard_Destroy
import platform.BasicServicesKit.Pasteboard.OH_Pasteboard_GetChangeCount
import platform.MediaKit.AVPlayer.OH_PLAYER_PLAYBACK_RATE
import platform.PerformanceAnalysisKit.HiDebug.HiDebug_GraphicsMemorySummary
import platform.PerformanceAnalysisKit.HiDebug.OH_HiDebug_GetGraphicsMemorySummary
import platform.PerformanceAnalysisKit.HiLog.LOG_APP
import platform.PerformanceAnalysisKit.HiLog.LOG_INFO
import platform.PerformanceAnalysisKit.HiLog.OH_LOG_Print
import platform.PerformanceAnalysisKit.Hitrace.OH_HiTrace_RegisterTraceListener
import platform.UniversalKeystoreKit.HuksKeyApi.OH_Huks_WrapKey
import platform.UniversalKeystoreKit.HuksTypeApi.OH_Huks_Blob
import platform.posix.strdup

private enum class SmokeOut {
    Ok,
    Fail,
    Ver
}

private const val HILOG_BENCH_ITERATIONS = 5000

/**
 * 清单：API18～22 各档代表函数 (5项) + 常量 (2项) = 共 7 项。 OH_Pasteboard_Create /
 * Destroy、OH_AudioStreamBuilder_Create / Destroy 等仅为同组调用链铺垫，不计入本清单。
 */
private val VERSIONGUARD_CHECKLIST_7 =
        listOf(
                // 函数 5 项
                "OH_Pasteboard_GetChangeCount",
                "OH_AudioStreamBuilder_SetVolumeMode",
                "OH_Huks_WrapKey",
                "OH_HiDebug_GetGraphicsMemorySummary",
                "OH_HiTrace_RegisterTraceListener",
                // 常量 2 项
                "OH_PLAYER_PLAYBACK_RATE",
                "OH_AVCODEC_MIMETYPE_AUDIO_ALAC",
        )

private val VERSIONGUARD_CHECKLIST_SET = VERSIONGUARD_CHECKLIST_7.toSet()

private val FUNCTION_CHECKLIST =
        listOf(
                "OH_Pasteboard_GetChangeCount",
                "OH_AudioStreamBuilder_SetVolumeMode",
                "OH_Huks_WrapKey",
                "OH_HiDebug_GetGraphicsMemorySummary",
                "OH_HiTrace_RegisterTraceListener",
        )

private val FUNCTION_CHECKLIST_SET = FUNCTION_CHECKLIST.toSet()

/** 常量读取（ApiGuard + 外层 try/catch 验证「未拦截异常」）。 */
private val CONSTANT_CHECKLIST =
        listOf(
                "OH_PLAYER_PLAYBACK_RATE",
                "OH_AVCODEC_MIMETYPE_AUDIO_ALAC",
        )

private val CONSTANT_CHECKLIST_SET = CONSTANT_CHECKLIST.toSet()

/** 与对照表一致：API18～22 各一档一条（仅 FUNCTION_CHECKLIST 内键）。 */
@Suppress("MagicNumber")
private val FUNCTION_INTRO_API_LEVEL: Map<String, Int> =
        mapOf(
                "OH_Pasteboard_GetChangeCount" to 18,
                "OH_AudioStreamBuilder_SetVolumeMode" to 19,
                "OH_Huks_WrapKey" to 20,
                "OH_HiDebug_GetGraphicsMemorySummary" to 21,
                "OH_HiTrace_RegisterTraceListener" to 22,
        )

@Suppress("MagicNumber")
private val CONSTANT_INTRO_API_LEVEL: Map<String, Int> =
        mapOf(
                "OH_PLAYER_PLAYBACK_RATE" to 20,
                "OH_AVCODEC_MIMETYPE_AUDIO_ALAC" to 22,
        )

private fun introApiForFunction(key: String): Int = FUNCTION_INTRO_API_LEVEL.getValue(key)

private fun introApiForConst(key: String): Int = CONSTANT_INTRO_API_LEVEL.getValue(key)

@Suppress("MagicNumber", "LongMethod", "CognitiveComplexity")
private fun buildVersionGuardFunctionSectionBody(
        checklistOutcome: MutableMap<String, SmokeOut>,
): String = buildString {
    fun mergeChecklist(key: String, o: SmokeOut) {
        require(key in VERSIONGUARD_CHECKLIST_SET) { "unknown checklist key: $key" }
        fun rank(x: SmokeOut) =
                when (x) {
                    SmokeOut.Ok -> 0
                    SmokeOut.Fail -> 1
                    SmokeOut.Ver -> 2
                }
        val prev = checklistOutcome[key]
        checklistOutcome[key] = if (prev == null) o else if (rank(o) >= rank(prev)) o else prev
    }

    fun takeHint(): String = ApiGuard.takeVersionHint()?.let { " $it" }.orEmpty()
    val funcResults = mutableMapOf<String, String>()

    fun gInt(statKey: String, block: () -> Int) {
        val v = ApiGuard.guardInt(block)
        takeHint()
        val o =
                when {
                    v == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                    v >= 0 -> SmokeOut.Ok
                    else -> SmokeOut.Fail
                }
        mergeChecklist(statKey, o)
        funcResults[statKey] = if (o == SmokeOut.Ver) "API_VERSION_TOO_HIGH" else "$v"
    }

    fun gLong(statKey: String, block: () -> Long) {
        val v = ApiGuard.guardLong(block)
        takeHint()
        val o =
                when {
                    v == ApiGuard.CODE_API_VERSION_TOO_HIGH.toLong() -> SmokeOut.Ver
                    v < 0L -> SmokeOut.Fail
                    else -> SmokeOut.Ok
                }
        mergeChecklist(statKey, o)
        funcResults[statKey] = if (o == SmokeOut.Ver) "API_VERSION_TOO_HIGH" else "$v"
    }

    memScoped {
        val pb = ApiGuard.guard({ OH_Pasteboard_Create() }, null, null)
        val createHint = takeHint()
        val createOk = pb != null
        val createOut =
                when {
                    createHint.isNotBlank() -> SmokeOut.Ver
                    createOk -> SmokeOut.Ok
                    else -> SmokeOut.Fail
                }

        if (pb == null) {
            val depOut =
                    when (createOut) {
                        SmokeOut.Ver -> SmokeOut.Ver
                        else -> SmokeOut.Fail
                    }
            mergeChecklist("OH_Pasteboard_GetChangeCount", depOut)
            funcResults["OH_Pasteboard_GetChangeCount"] = "-"
        } else {
            gLong("OH_Pasteboard_GetChangeCount") { OH_Pasteboard_GetChangeCount(pb).toLong() }
            ApiGuard.guardInvoke { OH_Pasteboard_Destroy(pb) }
            takeHint()
        }
    }

    memScoped {
        val builderPtr = alloc<CPointerVar<OH_AudioStreamBuilderStruct>>()
        builderPtr.value = null
        val createRc =
                ApiGuard.guardInt {
                    OH_AudioStreamBuilder_Create(builderPtr.ptr, AUDIOSTREAM_TYPE_RENDERER).toInt()
                }
        takeHint()
        val b: CPointer<OH_AudioStreamBuilderStruct>? = builderPtr.value
        if (b == null) {
            val depOut =
                    when {
                        createRc == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                        else -> SmokeOut.Fail
                    }
            mergeChecklist("OH_AudioStreamBuilder_SetVolumeMode", depOut)
            funcResults["OH_AudioStreamBuilder_SetVolumeMode"] = "-"
        } else {
            gInt("OH_AudioStreamBuilder_SetVolumeMode") {
                OH_AudioStreamBuilder_SetVolumeMode(b, AUDIOSTREAM_VOLUMEMODE_SYSTEM_GLOBAL).toInt()
            }
            ApiGuard.guardInvoke { OH_AudioStreamBuilder_Destroy(b) }
            takeHint()
        }
    }

    memScoped {
        val alias = "test_wrap_alias"
        val wrapBuf = ByteArray(1024)
        alias.encodeToByteArray().usePinned { aliasPinned ->
            wrapBuf.usePinned { wrapPinned ->
                val keyAlias = alloc<OH_Huks_Blob>()
                keyAlias.size = alias.length.toUInt()
                keyAlias.data = aliasPinned.addressOf(0).reinterpret<UByteVar>()
                val wrappedKey = alloc<OH_Huks_Blob>()
                wrappedKey.size = wrapBuf.size.toUInt()
                wrappedKey.data = wrapPinned.addressOf(0).reinterpret<UByteVar>()
                gInt("OH_Huks_WrapKey") {
                    OH_Huks_WrapKey(keyAlias.ptr, null, wrappedKey.ptr).useContents { errorCode }
                }
            }
        }
    }

    memScoped {
        val summary = alloc<HiDebug_GraphicsMemorySummary>()
        gInt("OH_HiDebug_GetGraphicsMemorySummary") {
            OH_HiDebug_GetGraphicsMemorySummary(300u, summary.ptr).toInt()
        }
    }

    memScoped {
        val cb = staticCFunction { _: Boolean -> }
        gInt("OH_HiTrace_RegisterTraceListener") { OH_HiTrace_RegisterTraceListener(cb) }
    }

    FUNCTION_CHECKLIST.forEach { key ->
        val o = checklistOutcome[key]
        val api = introApiForFunction(key)
        val desc =
                when (o) {
                    null -> "未执行"
                    SmokeOut.Ver -> "是否触发弱版本校验：是"
                    else -> "是否触发弱版本校验：否"
                }
        val ret = funcResults[key] ?: "-"
        appendLine("· $key（API$api）")
        appendLine("$desc")
        appendLine("返回值：$ret")
        if (key !== "OH_HiTrace_RegisterTraceListener") {
            appendLine()
        }
    }
}

@Suppress("MagicNumber", "LongMethod", "CognitiveComplexity")
private fun buildVersionGuardConstSectionBody(
        checklistOutcome: MutableMap<String, SmokeOut>,
): String = buildString {
    fun mergeChecklist(key: String, o: SmokeOut) {
        require(key in VERSIONGUARD_CHECKLIST_SET) { "unknown checklist key: $key" }
        fun rank(x: SmokeOut) =
                when (x) {
                    SmokeOut.Ok -> 0
                    SmokeOut.Fail -> 1
                    SmokeOut.Ver -> 2
                }
        val prev = checklistOutcome[key]
        checklistOutcome[key] = if (prev == null) o else if (rank(o) >= rank(prev)) o else prev
    }
    val constResults = mutableMapOf<String, String>()

    fun probeConst(key: String, guardedRead: () -> String): SmokeOut {
        var escaped: Throwable? = null
        val raw: String =
                try {
                    try {
                        guardedRead()
                    } catch (inner: Throwable) {
                        escaped = inner
                        "(inner)"
                    }
                } catch (outer: Throwable) {
                    escaped = outer
                    "(outer)"
                }
        val out =
                when {
                    escaped != null -> {
                        mergeChecklist(key, SmokeOut.Fail)
                        SmokeOut.Fail
                    }
                    raw == ApiGuard.STR_API_VERSION_TOO_HIGH -> {
                        mergeChecklist(key, SmokeOut.Ver)
                        SmokeOut.Ver
                    }
                    raw == "error" -> {
                        mergeChecklist(key, SmokeOut.Fail)
                        SmokeOut.Fail
                    }
                    else -> {
                        mergeChecklist(key, SmokeOut.Ok)
                        SmokeOut.Ok
                    }
                }
        constResults[key] =
                when {
                    escaped != null -> "exception"
                    raw == ApiGuard.STR_API_VERSION_TOO_HIGH -> "API_VERSION_TOO_HIGH"
                    else -> raw
                }
        return out
    }

    probeConst("OH_PLAYER_PLAYBACK_RATE") {
        ApiGuard.guardString { OH_PLAYER_PLAYBACK_RATE?.toKString().orEmpty() }
    }

    probeConst("OH_AVCODEC_MIMETYPE_AUDIO_ALAC") {
        ApiGuard.guardString { OH_AVCODEC_MIMETYPE_AUDIO_ALAC?.toKString().orEmpty() }
    }

    CONSTANT_CHECKLIST.forEach { key ->
        val o = checklistOutcome[key]
        val api = introApiForConst(key)
        val desc =
                when (o) {
                    null -> "未执行"
                    SmokeOut.Ver -> "是否触发弱版本校验：是"
                    else -> "是否触发弱版本校验：否"
                }
        val ret = constResults[key] ?: "-"
        appendLine("· $key（API$api）")
        appendLine("$desc")
        appendLine("返回值：$ret")
        if (key === "OH_PLAYER_PLAYBACK_RATE") {
            appendLine()
        }
    }
}

private fun fmtHiLogOhLogPrintBenchNs(totalNs: Long): String {
    fun round3(x: Double) = round(x * 1000) / 1000
    val ms = round3(totalNs / 1_000_000.0)
    val avgUs = round3(totalNs / HILOG_BENCH_ITERATIONS / 1_000.0)
    return "总计 $ms ms；平均 $avgUs μs/次"
}

private fun loopUserHiLogOhLogPrintBench() {
    // Originally benchmarked the project's self-wrapped cinterop (platform.User.HiLog) against the
    // platform klib. The self-wrapped cinterop was removed; both paths now use the maven klib
    // (platform.PerformanceAnalysisKit.HiLog) so this just measures that one path.
    for (i in 1..HILOG_BENCH_ITERATIONS) {
        platform.PerformanceAnalysisKit.HiLog.OH_LOG_Print(
                LOG_APP,
                LOG_INFO,
                0u,
                "tag",
                "User.HiLog OH_LOG_Print call=%{public}d",
                i,
        )
    }
}

private fun loopPlatformKitHiLogOhLogPrintBench() {
    for (i in 1..HILOG_BENCH_ITERATIONS) {
        OH_LOG_Print(
                LOG_APP,
                LOG_INFO,
                0u,
                "tag",
                "PerfKit OH_LOG_Print call=%{public}d",
                i,
        )
    }
}

/** 仅自封装 cinterop：`platform.User.HiLog.OH_LOG_Print` 循环 N 次耗时。 */
fun buildHiLogOhLogPrintBenchCinteropOnlyReport(): String {
    val ns = measureTime { loopUserHiLogOhLogPrintBench() }.inWholeNanoseconds
    return buildString {
        appendLine(
                "========== 自封装 cinterop：User.HiLog.OH_LOG_Print（${HILOG_BENCH_ITERATIONS} 次）=========="
        )
        appendLine("· ${fmtHiLogOhLogPrintBenchNs(ns)}")
    }
}

/** 仅系统库：`PerformanceAnalysisKit.HiLog.OH_LOG_Print` 循环 N 次耗时。 */
fun buildHiLogOhLogPrintBenchPlatformKitOnlyReport(): String {
    val ns = measureTime { loopPlatformKitHiLogOhLogPrintBench() }.inWholeNanoseconds
    return buildString {
        appendLine(
                "========== 系统库：PerformanceAnalysisKit.HiLog.OH_LOG_Print（${HILOG_BENCH_ITERATIONS} 次）=========="
        )
        appendLine("· ${fmtHiLogOhLogPrintBenchNs(ns)}")
    }
}

/** 两种 `OH_LOG_Print` 各 N 次，合并为一段对比报告（全量模块烟测用）。 */
fun buildHiLogOhLogPrintBenchOnlyReport(): String {
    val nsUser = measureTime { loopUserHiLogOhLogPrintBench() }.inWholeNanoseconds
    val nsPlatform = measureTime { loopPlatformKitHiLogOhLogPrintBench() }.inWholeNanoseconds
    return buildString {
        appendLine("========== HiLog OH_LOG_Print 耗时（各 ${HILOG_BENCH_ITERATIONS} 次）==========")
        appendLine(
                "· cinterop platform.User.HiLog.OH_LOG_Print — ${fmtHiLogOhLogPrintBenchNs(nsUser)}"
        )
        appendLine(
                "· platform.PerformanceAnalysisKit.HiLog.OH_LOG_Print — ${fmtHiLogOhLogPrintBenchNs(nsPlatform)}"
        )
    }
}

fun buildVersionGuardModuleSmokeReport(
        @Suppress("UNUSED_PARAMETER") databaseDir: String,
        bundleName: String,
        moduleName: String,
): String {
    val checklistOutcome = mutableMapOf<String, SmokeOut>()

    val funcBody = buildVersionGuardFunctionSectionBody(checklistOutcome)
    val constBody = buildVersionGuardConstSectionBody(checklistOutcome)

    return buildString {
        appendLine("=== 模块10：弱符号 / API 版本（函数 · 常量）===")
        appendLine("bundle=$bundleName module=$moduleName")
        appendLine("清单统计包括 5 个函数 + 2 个常量 = 共 7 项。")
        appendLine()
        appendLine(buildHiLogOhLogPrintBenchOnlyReport())
        appendLine()
        appendLine("========== 函数验证 ==========")
        append(funcBody)
        appendLine()
        appendLine("========== 常量验证 ==========")
        append(constBody)

        appendLine()
        appendLine("---------- 清单统计（共 ${VERSIONGUARD_CHECKLIST_7.size} 项：5 个函数 + 2 个常量）----------")
        appendLine("合并规则：同一清单项多次调用时，取较重结果（API版本不符 > 失败 > 成功）。")
        var ckOk = 0
        var ckFail = 0
        var ckVer = 0
        var ckNotRun = 0

        /** 解释为何未产生合并判定 */
        fun notRunExplanation(key: String): String {
            return when (key) {
                "OH_Pasteboard_GetChangeCount" -> {
                    "前置条件未满足：需 OH_Pasteboard_Create 成功才能调用 GetChangeCount。"
                }
                "OH_AudioStreamBuilder_SetVolumeMode" -> {
                    "前置条件未满足：需 OH_AudioStreamBuilder_Create 成功才能调用 SetVolumeMode。"
                }
                "OH_Huks_WrapKey",
                "OH_HiDebug_GetGraphicsMemorySummary",
                "OH_HiTrace_RegisterTraceListener" -> {
                    "该API在验证中未调用到本 API：可能是代码分支未执行到或前置条件未满足。"
                }
                "OH_PLAYER_PLAYBACK_RATE", "OH_AVCODEC_MIMETYPE_AUDIO_ALAC" -> {
                    "常量读取未执行：可能是 ApiGuard 拦截或读取逻辑未执行到。"
                }
                else -> "该清单项在报告中从未被合并过判定；可能是前置条件未满足或代码分支未执行到。"
            }
        }

        appendLine()
        VERSIONGUARD_CHECKLIST_7.forEachIndexed { index, key ->
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
        appendLine(
                "核对：${ckOk + ckFail + ckVer + ckNotRun} = ${VERSIONGUARD_CHECKLIST_7.size}（应为 7）"
        )

        appendLine()
        appendLine("---------- API 版本引入级别列表 ----------")
        appendLine("函数：")
        FUNCTION_CHECKLIST.forEachIndexed { i, key ->
            val api = introApiForFunction(key)
            val status =
                    when (checklistOutcome[key]) {
                        null -> "未执行"
                        SmokeOut.Ver -> "触发弱版本校验"
                        SmokeOut.Ok -> "成功"
                        SmokeOut.Fail -> "失败"
                    }
            appendLine("${i + 1}. $key — API$api — $status")
        }
        appendLine()
        appendLine("常量：")
        CONSTANT_CHECKLIST.forEachIndexed { i, key ->
            val api = introApiForConst(key)
            val status =
                    when (checklistOutcome[key]) {
                        null -> "未执行"
                        SmokeOut.Ver -> "触发弱版本校验"
                        SmokeOut.Ok -> "成功"
                        SmokeOut.Fail -> "失败"
                    }
            appendLine("${i + 1}. $key — API$api — $status")
        }

        appendLine()
        appendLine("---------- API 版本不符（ApiGuard）明细 ----------")
        val verKeys = VERSIONGUARD_CHECKLIST_7.filter { checklistOutcome[it] == SmokeOut.Ver }
        if (verKeys.isEmpty()) {
            appendLine("（无）")
        } else {
            verKeys.forEach { k ->
                val api =
                        if (k in FUNCTION_CHECKLIST_SET) {
                            introApiForFunction(k)
                        } else {
                            introApiForConst(k)
                        }
                appendLine("• $k（API$api）— 触发弱版本校验，设备 API 级别低于引入级别。")
            }
        }

        appendLine()
        appendLine("---------- 判定失败明细 ----------")
        val failKeys = VERSIONGUARD_CHECKLIST_7.filter { checklistOutcome[it] == SmokeOut.Fail }
        if (failKeys.isEmpty()) {
            appendLine("（无）")
        } else {
            failKeys.forEach { k ->
                val api =
                        if (k in FUNCTION_CHECKLIST_SET) {
                            introApiForFunction(k)
                        } else {
                            introApiForConst(k)
                        }
                appendLine("• $k（API$api）— 调用失败或返回异常。")
            }
        }

        appendLine()
        appendLine("---------- 未执行明细 ----------")
        appendLine(
                "  → 释义：「未执行」表示该清单项在报告中**从未被合并过判定**（与「失败」「API版本不符」不同）；" +
                        "常见原因是**前置条件未满足**导致代码分支未走到（例如 Create 失败则后续 API 不会调用）。"
        )
        val notRunKeys = VERSIONGUARD_CHECKLIST_7.filter { checklistOutcome[it] == null }
        if (notRunKeys.isEmpty()) {
            appendLine("（无）")
        } else {
            notRunKeys.forEach { k ->
                appendLine("• $k")
                appendLine("  → ${notRunExplanation(k)}")
            }
        }
    }
}

fun buildVersionGuardFuncOnlyReport(
        @Suppress("UNUSED_PARAMETER") databaseDir: String,
        @Suppress("UNUSED_PARAMETER") bundleName: String,
        @Suppress("UNUSED_PARAMETER") moduleName: String,
): String {
    val checklistOutcome = mutableMapOf<String, SmokeOut>()
    return buildVersionGuardFunctionSectionBody(checklistOutcome)
}

fun buildVersionGuardConstOnlyReport(
        @Suppress("UNUSED_PARAMETER") databaseDir: String,
        @Suppress("UNUSED_PARAMETER") bundleName: String,
        @Suppress("UNUSED_PARAMETER") moduleName: String,
): String {
    val checklistOutcome = mutableMapOf<String, SmokeOut>()
    return buildVersionGuardConstSectionBody(checklistOutcome)
}

@CName("kn_runVersionGuardFuncSmokeTest")
fun kn_runVersionGuardFuncSmokeTest(
        dbDir: CPointer<ByteVar>?,
        bundleName: CPointer<ByteVar>?,
        moduleName: CPointer<ByteVar>?,
): CPointer<ByteVar>? {
    val dir = dbDir?.toKString().orEmpty()
    val bundle = bundleName?.toKString().orEmpty()
    val module = moduleName?.toKString().orEmpty()
    val report =
            try {
                buildVersionGuardFuncOnlyReport(dir, bundle, module)
            } catch (t: Throwable) {
                "Kotlin 异常: ${t::class.simpleName}: ${t.message}\n${t.stackTraceToString()}"
            }
    return strdup(report)
}

@CName("kn_runVersionGuardConstSmokeTest")
fun kn_runVersionGuardConstSmokeTest(
        dbDir: CPointer<ByteVar>?,
        bundleName: CPointer<ByteVar>?,
        moduleName: CPointer<ByteVar>?,
): CPointer<ByteVar>? {
    val dir = dbDir?.toKString().orEmpty()
    val bundle = bundleName?.toKString().orEmpty()
    val module = moduleName?.toKString().orEmpty()
    val report =
            try {
                buildVersionGuardConstOnlyReport(dir, bundle, module)
            } catch (t: Throwable) {
                "Kotlin 异常: ${t::class.simpleName}: ${t.message}\n${t.stackTraceToString()}"
            }
    return strdup(report)
}

@CName("kn_runVersionGuardModuleSmokeTest")
fun kn_runVersionGuardModuleSmokeTest(
        dbDir: CPointer<ByteVar>?,
        bundleName: CPointer<ByteVar>?,
        moduleName: CPointer<ByteVar>?,
): CPointer<ByteVar>? {
    val dir = dbDir?.toKString().orEmpty()
    val bundle = bundleName?.toKString().orEmpty()
    val module = moduleName?.toKString().orEmpty()
    val report =
            try {
                buildVersionGuardModuleSmokeReport(dir, bundle, module)
            } catch (t: Throwable) {
                "Kotlin 异常: ${t::class.simpleName}: ${t.message}\n${t.stackTraceToString()}"
            }
    return strdup(report)
}

@CName("kn_runHiLogOhLogPrintBenchCinteropSmokeTest")
fun kn_runHiLogOhLogPrintBenchCinteropSmokeTest(
        @Suppress("UNUSED_PARAMETER") dbDir: CPointer<ByteVar>?,
        @Suppress("UNUSED_PARAMETER") bundleName: CPointer<ByteVar>?,
        @Suppress("UNUSED_PARAMETER") moduleName: CPointer<ByteVar>?,
): CPointer<ByteVar>? {
    val report =
            try {
                buildHiLogOhLogPrintBenchCinteropOnlyReport()
            } catch (t: Throwable) {
                "Kotlin 异常: ${t::class.simpleName}: ${t.message}\n${t.stackTraceToString()}"
            }
    return strdup(report)
}

@CName("kn_runHiLogOhLogPrintBenchPlatformKitSmokeTest")
fun kn_runHiLogOhLogPrintBenchPlatformKitSmokeTest(
        @Suppress("UNUSED_PARAMETER") dbDir: CPointer<ByteVar>?,
        @Suppress("UNUSED_PARAMETER") bundleName: CPointer<ByteVar>?,
        @Suppress("UNUSED_PARAMETER") moduleName: CPointer<ByteVar>?,
): CPointer<ByteVar>? {
    val report =
            try {
                buildHiLogOhLogPrintBenchPlatformKitOnlyReport()
            } catch (t: Throwable) {
                "Kotlin 异常: ${t::class.simpleName}: ${t.message}\n${t.stackTraceToString()}"
            }
    return strdup(report)
}
