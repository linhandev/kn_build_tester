@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.experimental.ExperimentalNativeApi::class,
)

package com.example.platform.Hms

import com.example.test.common.ApiGuard
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.CANNKit.CANN.HMS_HiAI_GetVersion
import platform.RemoteCommunicationKit.RemoteCommunication.HMS_Rcp_CreateForm
import platform.RemoteCommunicationKit.RemoteCommunication.HMS_Rcp_DestroyForm
import platform.posix.strdup
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName

// HMS 扩展 Kit smoke 验证:调两个 hms API,验证 hms-capi 坐标能被 consumer 实际 link。
// - HMS_Rcp_CreateForm/DestroyForm(platform.RemoteCommunicationKit.RemoteCommunication):不依赖 ohos。
// - HMS_HiAI_GetVersion(platform.CANNKit.CANN):依赖 ohos(def depends = NeuralNetworkRuntime),
//   触发跨坐标 depends 解析 → 验证 hms-capi POM depend ohos-capi 的传递。

private enum class SmokeOut { Ok, Fail, Ver }

private val HMS_CHECKLIST = listOf(
    "HMS_Rcp_CreateForm",
    "HMS_HiAI_GetVersion",
)
private val HMS_CHECKLIST_SET = HMS_CHECKLIST.toSet()

private fun hmsStatKeyFromLabel(label: String): String? =
    label.substringBefore("(").trim().takeIf { it in HMS_CHECKLIST_SET }

@Suppress("MagicNumber", "LongMethod", "CognitiveComplexity")
fun buildHmsModuleSmokeReport(
    @Suppress("UNUSED_PARAMETER") databaseDir: String,
    bundleName: String,
    moduleName: String,
): String {
    val failDetails = mutableListOf<String>()
    val verDetails = mutableListOf<String>()
    val checklistOutcome = mutableMapOf<String, SmokeOut>()

    fun mergeChecklist(key: String, o: SmokeOut) {
        require(key in HMS_CHECKLIST_SET) { "unknown checklist key: $key" }
        fun rank(x: SmokeOut) = when (x) {
            SmokeOut.Ok -> 0; SmokeOut.Fail -> 1; SmokeOut.Ver -> 2
        }
        val prev = checklistOutcome[key]
        checklistOutcome[key] = if (prev == null) o else if (rank(o) >= rank(prev)) o else prev
    }

    return buildString {
        appendLine("=== HMS 扩展 Kit CAPI 验证(platform.RemoteCommunicationKit.RemoteCommunication / platform.CANNKit.CANN)===")
        appendLine("bundle=$bundleName module=$moduleName")
        appendLine("场景:Rcp CreateForm(不依赖 ohos)+ CANN HiAI 版本(依赖 ohos NeuralNetworkRuntime,验 POM 传递)")
        appendLine()

        val body = buildString {
            fun record(name: String, raw: String, o: SmokeOut, extra: String? = null, statKey: String? = null) {
                val key = statKey ?: hmsStatKeyFromLabel(name)
                if (key != null) mergeChecklist(key, o)
                appendLine("$name: $raw")
                appendLine("  → ${extra?.trim().orEmpty()}")
                when (o) {
                    SmokeOut.Fail -> failDetails.add("• $name：结果 $raw。${extra?.trim().orEmpty()}")
                    SmokeOut.Ver -> verDetails.add("• $name：结果 $raw。${extra?.trim().orEmpty()}")
                    SmokeOut.Ok -> Unit
                }
            }

            fun gInt(name: String, statKey: String, ok: (Int) -> Boolean = { it >= 0 }, block: () -> Int) {
                val v = ApiGuard.guardInt(block)
                val verHint = ApiGuard.takeVersionHint()
                val raw = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) "API_VERSION_TOO_HIGH" else "$v"
                val o = when {
                    v == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                    ok(v) -> SmokeOut.Ok
                    else -> SmokeOut.Fail
                }
                val extra = buildString {
                    append(if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) "释义：API_VERSION_TOO_HIGH（ApiGuard）。" else "释义：返回 $v。")
                    if (!verHint.isNullOrBlank()) append(" ").append(verHint)
                }
                record(name, raw, o, extra, statKey)
            }

            // 1. RemoteCommunication(不依赖 ohos):create form + destroy form,opaque 指针不碰 struct。
            run {
                val name = "HMS_Rcp_CreateForm() + HMS_Rcp_DestroyForm() — create/destroy 对"
                val statKey = "HMS_Rcp_CreateForm"
                val verHint = ApiGuard.takeVersionHint()
                var raw: String
                var o: SmokeOut
                var extra: String
                try {
                    val form = ApiGuard.guard(
                        { HMS_Rcp_CreateForm() },
                        null,
                        null,
                    )
                    if (form == null) {
                        raw = "API_VERSION_TOO_HIGH 或异常"
                        o = SmokeOut.Ver
                        extra = "释义：API_VERSION_TOO_HIGH（ApiGuard）。${verHint.orEmpty()}"
                    } else {
                        HMS_Rcp_DestroyForm(form)
                        raw = "create→非空,destroy→完成"
                        o = SmokeOut.Ok
                        extra = "释义：Rcp_Form create/destroy 成功（opaque 指针,不碰 struct 字段）。${verHint.orEmpty()}"
                    }
                } catch (t: Throwable) {
                    raw = "异常"
                    o = if (t is IllegalStateException) SmokeOut.Ver else SmokeOut.Fail
                    extra = "释义：${t::class.simpleName}: ${t.message}"
                }
                record(name, raw, o, extra, statKey)
            }

            // 2. CANN HiAI_GetVersion(依赖 ohos 的 NeuralNetworkRuntime):返回版本串。
            // 用 guardString 包:CPointer<ByteVar>? → toKString,版本不匹配返回占位串。
            val verHint = ApiGuard.takeVersionHint()
            val hiAiVer = ApiGuard.guardString { HMS_HiAI_GetVersion()?.toKString().orEmpty() }
            val hiAiRaw = hiAiVer.ifBlank { "(空)" }
            val hiAiO = when {
                hiAiVer == ApiGuard.STR_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                hiAiVer.isNotEmpty() -> SmokeOut.Ok
                else -> SmokeOut.Fail
            }
            val hiAiExtra = buildString {
                append("释义：HiAI 版本串。")
                if (!verHint.isNullOrBlank()) append(" ").append(verHint)
            }
            record(
                "HMS_HiAI_GetVersion() — 返回 const char*",
                hiAiRaw,
                hiAiO,
                hiAiExtra,
                "HMS_HiAI_GetVersion",
            )
        }

        append(body)
        appendLine()
        appendLine("---------- 清单统计（共 ${HMS_CHECKLIST.size} 项）----------")
        var ckOk = 0; var ckFail = 0; var ckVer = 0; var ckNotRun = 0
        HMS_CHECKLIST.forEachIndexed { index, key ->
            val o = checklistOutcome[key]
            val label = when (o) {
                null -> { ckNotRun++; "未执行" }
                SmokeOut.Ok -> { ckOk++; "成功" }
                SmokeOut.Fail -> { ckFail++; "失败" }
                SmokeOut.Ver -> { ckVer++; "API版本不符" }
            }
            appendLine("${index + 1}. $key — $label")
        }
        appendLine()
        appendLine("清单汇总：成功=$ckOk  失败=$ckFail  API版本不符=$ckVer  未执行=$ckNotRun")
        appendLine()
        appendLine("---------- 判定失败明细 ----------")
        if (failDetails.isEmpty()) appendLine("（无）") else failDetails.forEach { appendLine(it) }
        appendLine()
        appendLine("---------- API 版本不符（ApiGuard）明细 ----------")
        if (verDetails.isEmpty()) appendLine("（无）") else verDetails.forEach { appendLine(it) }
    }
}

@CName("kn_runHmsModuleSmokeTest")
fun kn_runHmsModuleSmokeTest(
    dbDir: CPointer<ByteVar>?,
    bundleName: CPointer<ByteVar>?,
    moduleName: CPointer<ByteVar>?,
): CPointer<ByteVar>? {
    val dir = dbDir?.toKString().orEmpty()
    val bundle = bundleName?.toKString().orEmpty()
    val module = moduleName?.toKString().orEmpty()
    val report = try {
        buildHmsModuleSmokeReport(dir, bundle, module)
    } catch (t: Throwable) {
        "Kotlin 异常: ${t::class.simpleName}: ${t.message}\n${t.stackTraceToString()}"
    }
    return strdup(report)
}
