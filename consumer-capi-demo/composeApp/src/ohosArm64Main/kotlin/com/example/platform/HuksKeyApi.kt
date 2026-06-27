@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.experimental.ExperimentalNativeApi::class,
)

package com.example.platform.HuksKeyApi

import com.example.test.common.ApiGuard
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName
import kotlinx.cinterop.*
import platform.UniversalKeystoreKit.HuksKeyApi.OH_Huks_GetSdkVersion
import platform.UniversalKeystoreKit.HuksParamSetApi.OH_Huks_AddParams
import platform.UniversalKeystoreKit.HuksParamSetApi.OH_Huks_FreeParamSet
import platform.UniversalKeystoreKit.HuksParamSetApi.OH_Huks_InitParamSet
import platform.UniversalKeystoreKit.HuksTypeApi.OH_HUKS_ALG_RSA
import platform.UniversalKeystoreKit.HuksTypeApi.OH_HUKS_SUCCESS
import platform.UniversalKeystoreKit.HuksTypeApi.OH_HUKS_TAG_ALGORITHM
import platform.UniversalKeystoreKit.HuksTypeApi.OH_Huks_Blob
import platform.UniversalKeystoreKit.HuksTypeApi.OH_Huks_Param
import platform.UniversalKeystoreKit.HuksTypeApi.OH_Huks_ParamSet
import platform.UniversalKeystoreKit.HuksTypeApi.OH_Huks_Result
import platform.posix.strdup

/**
 * 场景一：获取 HUKS SDK 版本并构造参数集（RSA 算法标签）。
 *
 * - `OH_Huks_GetSdkVersion` 等密钥操作 API 在 [platform.UniversalKeystoreKit.HuksKeyApi]。
 * - `OH_Huks_InitParamSet` / `OH_Huks_AddParams` / `OH_Huks_FreeParamSet` 在平台 klib 中位于 **HuksParamSetApi**（与头文件 `native_huks_api.h` / `native_huks_param.h` 分工一致）。
 *
 * 官方：[capi-native-huks-api-h](https://developer.huawei.com/consumer/cn/doc/harmonyos-references/capi-native-huks-api-h)
 */

private enum class SmokeOut { Ok, Fail, Ver }

/** 场景一最小清单：4 个函数（与需求「最少执行 4 个函数」一致）。 */
private val HUKS_SCENARIO1_CHECKLIST = listOf(
    "OH_Huks_GetSdkVersion",
    "OH_Huks_InitParamSet",
    "OH_Huks_AddParams",
    "OH_Huks_FreeParamSet",
)

private val HUKS_SCENARIO1_SET = HUKS_SCENARIO1_CHECKLIST.toSet()

private fun huksStatKeyFromLabel(label: String): String? =
    label.substringBefore("(").trim().takeIf { it in HUKS_SCENARIO1_SET }

@Suppress("MagicNumber", "LongMethod", "CognitiveComplexity")
fun buildHuksKeyApiModuleSmokeReport(
    @Suppress("UNUSED_PARAMETER") databaseDir: String,
    bundleName: String,
    moduleName: String,
): String {
    val failDetails = mutableListOf<String>()
    val verDetails = mutableListOf<String>()
    val checklistOutcome = mutableMapOf<String, SmokeOut>()
    var detailLineCount = 0

    fun mergeChecklist(key: String, o: SmokeOut) {
        require(key in HUKS_SCENARIO1_SET) { "unknown checklist key: $key" }
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

    fun zhHuksErrCode(code: Int): String = when {
        code == ApiGuard.CODE_API_VERSION_TOO_HIGH ->
            "释义：API_VERSION_TOO_HIGH（ApiGuard）— 已拦截，未进入 native；详见报告末尾「API 版本不符」明细。"
        code == OH_HUKS_SUCCESS.toInt() ->
            "释义：errorCode=0 即 OH_HUKS_SUCCESS，操作成功（OH_Huks_ErrCode / native_huks_type.h）。"
        code == 401 ->
            "释义：401 OH_HUKS_ERR_CODE_ILLEGAL_ARGUMENT，参数非法（如 Blob.size 过小、指针为空）。"
        code == 201 ->
            "释义：201 OH_HUKS_ERR_CODE_PERMISSION_FAIL，权限校验失败。"
        code == 801 ->
            "释义：801 OH_HUKS_ERR_CODE_NOT_SUPPORTED_API，当前环境不支持该 API。"
        code in 12_000_001..12_000_025 ->
            "释义：12xxxxxx 区间为 HUKS 业务错误码，请对照 OH_Huks_ErrCode。"
        else ->
            "释义：非 0 请对照官方 OH_Huks_ErrCode（capi-native-huks-api-h）。"
    }

    val nativeDetailText = memScoped {
        val detailSb = StringBuilder()
        fun record(
            name: String,
            raw: String,
            o: SmokeOut,
            extra: String? = null,
            statKey: String? = null,
            omitFailDetail: Boolean = false,
        ) {
            detailLineCount++
            val key = statKey ?: huksStatKeyFromLabel(name)
            if (key != null) {
                mergeChecklist(key, o)
            }
            detailSb.appendLine("$name: $raw")
            detailSb.appendLine("  → ${extra ?: ""}")
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

        /** 返回 OH_Huks_Result 的 C API：整段包在 guardInt 内用 useContents 读 errorCode。 */
        fun gHuksResultValue(
            name: String,
            statKey: String,
            block: () -> kotlinx.cinterop.CValue<OH_Huks_Result>,
        ) {
            val v = ApiGuard.guardInt { block().useContents { errorCode } }
            val verHint = ApiGuard.takeVersionHint()
            val raw = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) "API_VERSION_TOO_HIGH" else "$v"
            val o = when {
                v == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                v == OH_HUKS_SUCCESS.toInt() -> SmokeOut.Ok
                else -> SmokeOut.Fail
            }
            val extra = buildString {
                append(zhHuksErrCode(v))
                if (!verHint.isNullOrBlank()) {
                    append(" ")
                    append(verHint)
                }
            }
            record(name, raw, o, extra, statKey)
        }

        fun gInvokeFree(name: String, statKey: String, block: () -> Unit) {
            val v = ApiGuard.guardInvoke(block)
            val verHint = ApiGuard.takeVersionHint()
            val raw = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) "API_VERSION_TOO_HIGH" else "$v"
            val o = when {
                v == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                v == 0 -> SmokeOut.Ok
                else -> SmokeOut.Fail
            }
            val extra = buildString {
                append(
                    when (v) {
                        ApiGuard.CODE_API_VERSION_TOO_HIGH -> zhHuksErrCode(v)
                        0 -> "释义：0 — FreeParamSet 无 OH_Huks_Result 返回，guardInvoke 表示未抛异常。"
                        -1 -> "释义：-1 — 释放过程发生非版本类异常。"
                        else -> zhHuksErrCode(v)
                    },
                )
                if (!verHint.isNullOrBlank()) {
                    append(" ")
                    append(verHint)
                }
            }
            record(name, raw, o, extra, statKey)
        }

        val sdkBuf = ByteArray(64)
        var verPreview = ""
        sdkBuf.usePinned { pinned ->
            val versionBlob = alloc<OH_Huks_Blob>()
            versionBlob.size = 64u
            versionBlob.data = pinned.addressOf(0).reinterpret<UByteVar>()

            gHuksResultValue(
                "OH_Huks_GetSdkVersion(sdkVersion.size=64, sdkVersion.data=pinnedByteArray)",
                "OH_Huks_GetSdkVersion",
            ) {
                OH_Huks_GetSdkVersion(versionBlob.ptr)
            }

            verPreview = buildString {
                for (i in sdkBuf.indices) {
                    val b = sdkBuf[i].toInt() and 0xff
                    if (b == 0) break
                    if (b in 32..126) append(b.toChar())
                }
            }.ifBlank { "(缓冲区无可见 ASCII，可能仍以 errorCode 成功)" }
        }
        detailSb.appendLine("  → 释义：SDK 版本串预览（自 sdkVersion 缓冲区）：$verPreview")
        detailSb.appendLine()

        val paramSetVar = alloc<CPointerVar<OH_Huks_ParamSet>>()
        paramSetVar.value = null

        gHuksResultValue(
            "OH_Huks_InitParamSet(paramSetVar.ptr)",
            "OH_Huks_InitParamSet",
        ) {
            OH_Huks_InitParamSet(paramSetVar.ptr)
        }

        val paramSetPtr = paramSetVar.value
        if (paramSetPtr != null) {
            val huksParams = alloc<OH_Huks_Param>()
            huksParams.tag = OH_HUKS_TAG_ALGORITHM
            huksParams.uint32Param = OH_HUKS_ALG_RSA

            gHuksResultValue(
                "OH_Huks_AddParams(paramSet, params=[{tag=OH_HUKS_TAG_ALGORITHM, uint32=OH_HUKS_ALG_RSA}], paramCnt=1)",
                "OH_Huks_AddParams",
            ) {
                OH_Huks_AddParams(paramSetPtr, huksParams.ptr, 1u)
            }
        } else {
            record(
                "OH_Huks_AddParams(跳过: InitParamSet 未得到 paramSet)",
                "skip",
                SmokeOut.Fail,
                "释义：paramSet 为 null，无法 AddParams。",
                "OH_Huks_AddParams",
            )
        }

        gInvokeFree(
            "OH_Huks_FreeParamSet(&paramSet)",
            "OH_Huks_FreeParamSet",
        ) {
            OH_Huks_FreeParamSet(paramSetVar.ptr)
        }

        detailSb.toString()
    }

    return buildString {
        appendLine("=== HuksKeyApi 密钥管理 CAPI 验证（场景一：SDK 版本 + ParamSet）===")
        appendLine("bundle=$bundleName module=$moduleName")
        appendLine(
            "API 来源：HuksKeyApi.GetSdkVersion；ParamSet 系列见 HuksParamSetApi（与头文件 native_huks_param.h 一致）。",
        )
        appendLine("清单 4 项：GetSdkVersion → InitParamSet → AddParams(TAG_ALGORITHM=RSA) → FreeParamSet。")
        appendLine()
        append(nativeDetailText)
        appendLine()
        appendLine("---------- 清单统计（场景一，共 ${HUKS_SCENARIO1_CHECKLIST.size} 项）----------")
        var ckOk = 0
        var ckFail = 0
        var ckVer = 0
        var ckNotRun = 0
        HUKS_SCENARIO1_CHECKLIST.forEachIndexed { index, key ->
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
        appendLine("核对：${ckOk + ckFail + ckVer + ckNotRun} = ${HUKS_SCENARIO1_CHECKLIST.size}（应为 4）")
        appendLine()
        // appendLine("明细输出行数：$detailLineCount")
        // appendLine()
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
        val notRunKeys = HUKS_SCENARIO1_CHECKLIST.filter { checklistOutcome[it] == null }
        if (notRunKeys.isEmpty()) {
            appendLine("（无）")
        } else {
            notRunKeys.forEach { k ->
                appendLine("• $k — 前置失败或未调用 record 合并。")
            }
        }
    }
}

@CName("kn_runHuksKeyModuleSmokeTest")
fun kn_runHuksKeyModuleSmokeTest(
    dbDir: CPointer<ByteVar>?,
    bundleName: CPointer<ByteVar>?,
    moduleName: CPointer<ByteVar>?,
): CPointer<ByteVar>? {
    val dir = dbDir?.toKString().orEmpty()
    val bundle = bundleName?.toKString().orEmpty()
    val module = moduleName?.toKString().orEmpty()
    val report = try {
        buildHuksKeyApiModuleSmokeReport(dir, bundle, module)
    } catch (t: Throwable) {
        "Kotlin 异常: ${t::class.simpleName}: ${t.message}\n${t.stackTraceToString()}"
    }
    return strdup(report)
}
