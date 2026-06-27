@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.experimental.ExperimentalNativeApi::class,
)

package com.example.platform.CommonEvent

import com.example.test.common.ApiGuard
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName
import cnames.structs.CommonEvent_PublishInfo
import cnames.structs.CommonEvent_RcvData
import cnames.structs.CommonEvent_SubscribeInfo
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import platform.BasicServicesKit.OH_CommonEvent.*
import platform.posix.strdup
import platform.posix.usleep

/**
 * 订阅回调须为文件级 [staticCFunction]；各 Get* 与 HasKey 的清单项通过 [ceEnqueueRecord] 写入待合并队列，主流程再 flush 统一 [record]（与 RDB 损坏回调汇总模式一致）。
 *
 * 文档：[capi-oh-commonevent-h](https://developer.huawei.com/consumer/cn/doc/harmonyos-references/capi-oh-commonevent-h)
 */

private enum class SmokeOut { Ok, Fail, Ver }

private data class CePendingRecord(
    val nameWithArgs: String,
    val raw: String,
    val o: SmokeOut,
    val extra: String,
)

private val cePendingRecords = mutableListOf<CePendingRecord>()

/** 回调线程写入；主线程 flush 时消费并清空。 */
private fun ceEnqueueRecord(nameWithArgs: String, raw: String, o: SmokeOut, extra: String) {
    cePendingRecords.add(CePendingRecord(nameWithArgs, raw, o, extra))
}

private fun flushCePendingRecords(
    record: (name: String, raw: String, o: SmokeOut, extra: String?, statKey: String?) -> Unit,
): Int {
    val n = cePendingRecords.size
    cePendingRecords.forEach { r ->
        record(r.nameWithArgs, r.raw, r.o, r.extra, null)
    }
    cePendingRecords.clear()
    return n
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
@Suppress("unused")
private val commonEventReceiveCallback =
    staticCFunction { data: CPointer<CommonEvent_RcvData>? ->
        if (data == null) {
            ceEnqueueRecord(
                "[回调→页面] OH_CommonEvent_CreateSubscriber(staticCFunction)",
                "rcvData=null",
                SmokeOut.Fail,
                "释义：本行由订阅回调写入，与界面展示共用同一份报告正文；无 rcvData 时不继续 Get*。",
            )
            return@staticCFunction
        }
        try {
            ceEnqueueRecord(
                "[回调→页面] OH_CommonEvent_CreateSubscriber(staticCFunction)",
                "已触发",
                SmokeOut.Ok,
                "释义：本行由订阅回调写入报告，随 NAPI 返回后在当前页 Text 中显示；" +
                    "能看到此行即表示传给 OH_CommonEvent_CreateSubscriber 的函数指针已被 native 调用。",
            )
            val evPtr = OH_CommonEvent_GetEventFromRcvData(data)
            val ev = evPtr?.toKString()
            ceEnqueueRecord(
                "OH_CommonEvent_GetEventFromRcvData(rcvData=non-null)",
                ev ?: "null",
                SmokeOut.Ok,
                "释义：返回事件名字符串指针转 UTF-8；内容为 null 仅表示 C 侧返回空串/无名称，调用本身成功。",
            )

            val bnPtr = OH_CommonEvent_GetBundleNameFromRcvData(data)
            val bn = bnPtr?.toKString()
            ceEnqueueRecord(
                "OH_CommonEvent_GetBundleNameFromRcvData(rcvData=non-null)",
                bn ?: "null",
                SmokeOut.Ok,
                "释义：发布方 bundleName；与 SetPublisherBundleName 过滤相关。",
            )

            val para = OH_CommonEvent_GetParametersFromRcvData(data)
            ceEnqueueRecord(
                "OH_CommonEvent_GetParametersFromRcvData(rcvData=non-null)",
                if (para != null) "non-null" else "null",
                SmokeOut.Ok,
                "释义：首次简单 Publish 常为 null（无附加 Parameters）；PublishWithInfo 后应为 non-null。",
            )

            if (para != null) {
                fun hasK(key: String) = OH_CommonEvent_HasKeyInParameters(para, key)
                ceEnqueueRecord(
                    "OH_CommonEvent_HasKeyInParameters(para, key=\"longKey\")",
                    "${hasK("longKey")}",
                    SmokeOut.Ok,
                    "释义：是否包含 longKey。",
                )
                ceEnqueueRecord(
                    "OH_CommonEvent_HasKeyInParameters(para, key=\"dKey\")",
                    "${hasK("dKey")}",
                    SmokeOut.Ok,
                    "释义：是否包含 dKey。",
                )
                ceEnqueueRecord(
                    "OH_CommonEvent_HasKeyInParameters(para, key=\"cKey\")",
                    "${hasK("cKey")}",
                    SmokeOut.Ok,
                    "释义：是否包含 cKey。",
                )
                ceEnqueueRecord(
                    "OH_CommonEvent_HasKeyInParameters(para, key=\"boolKey\")",
                    "${hasK("boolKey")}",
                    SmokeOut.Ok,
                    "释义：是否包含 boolKey（演示用，清单外亦会 SetBoolToParameters）。",
                )

                val lg = OH_CommonEvent_GetLongFromParameters(para, "longKey", 0L)
                ceEnqueueRecord(
                    "OH_CommonEvent_GetLongFromParameters(para, key=\"longKey\", default=0L)",
                    "$lg",
                    SmokeOut.Ok,
                    "释义：读取 long；应与 SetLongToParameters 一致。",
                )
                val db = OH_CommonEvent_GetDoubleFromParameters(para, "dKey", 0.0)
                ceEnqueueRecord(
                    "OH_CommonEvent_GetDoubleFromParameters(para, key=\"dKey\", default=0.0)",
                    "$db",
                    SmokeOut.Ok,
                    "释义：读取 double；应与 SetDoubleToParameters 一致。",
                )
                val ch = OH_CommonEvent_GetCharFromParameters(para, "cKey", 'x'.code.toByte())
                ceEnqueueRecord(
                    "OH_CommonEvent_GetCharFromParameters(para, key=\"cKey\", default='x')",
                    "$ch",
                    SmokeOut.Ok,
                    "释义：读取 char；应与 SetCharToParameters 一致。",
                )
                val bl = OH_CommonEvent_GetBoolFromParameters(para, "boolKey", false)
                ceEnqueueRecord(
                    "OH_CommonEvent_GetBoolFromParameters(para, key=\"boolKey\", default=false)",
                    "$bl",
                    SmokeOut.Ok,
                    "释义：读取 bool；清单外步骤 SetBoolToParameters 写入。",
                )
            }
        } catch (t: Throwable) {
            ceEnqueueRecord(
                "OH_CommonEvent_订阅回调异常",
                "${t::class.simpleName}: ${t.message}",
                SmokeOut.Fail,
                "释义：回调内 Kotlin 异常。",
            )
        }
    }

/**
 * 图 39–61 共 **23** 项（与需求清单一致）：主流程 + 回调内 Get 系列与 HasKey（不含 SetBoolToParameters、DestroyParameters、DestroyPublishInfo，后三者可在报告中作为「清单外步骤」展示）。
 */
private val COMMON_EVENT_CHECKLIST_23 = listOf(
    "OH_CommonEvent_CreateSubscribeInfo",
    "OH_CommonEvent_SetPublisherPermission",
    "OH_CommonEvent_SetPublisherBundleName",
    "OH_CommonEvent_CreateSubscriber",
    "OH_CommonEvent_Subscribe",
    "OH_CommonEvent_Publish",
    "OH_CommonEvent_GetEventFromRcvData",
    "OH_CommonEvent_GetBundleNameFromRcvData",
    "OH_CommonEvent_GetParametersFromRcvData",
    "OH_CommonEvent_HasKeyInParameters",
    "OH_CommonEvent_GetLongFromParameters",
    "OH_CommonEvent_GetDoubleFromParameters",
    "OH_CommonEvent_GetCharFromParameters",
    "OH_CommonEvent_UnSubscribe",
    "OH_CommonEvent_DestroySubscriber",
    "OH_CommonEvent_DestroySubscribeInfo",
    "OH_CommonEvent_CreateParameters",
    "OH_CommonEvent_SetDoubleToParameters",
    "OH_CommonEvent_SetLongToParameters",
    "OH_CommonEvent_SetCharToParameters",
    "OH_CommonEvent_CreatePublishInfo",
    "OH_CommonEvent_SetPublishInfoParameters",
    "OH_CommonEvent_PublishWithInfo",
)

private val COMMON_EVENT_CHECKLIST_SET = COMMON_EVENT_CHECKLIST_23.toSet()

private fun ceStatKeyFromLabel(label: String): String? =
    label.substringBefore("(").trim().takeIf { it in COMMON_EVENT_CHECKLIST_SET }

@Suppress("MagicNumber", "LongMethod", "CognitiveComplexity")
fun buildCommonEventModuleSmokeReport(
    @Suppress("UNUSED_PARAMETER") databaseDir: String,
    bundleName: String,
    moduleName: String,
): String {
    val failDetails = mutableListOf<String>()
    val verDetails = mutableListOf<String>()
    val checklistOutcome = mutableMapOf<String, SmokeOut>()
    var detailLineCount = 0

    cePendingRecords.clear()

    val bundleResolved = bundleName.ifBlank { "com.example.harmonyapp" }
    val moduleResolved = moduleName.ifBlank { "entry" }

    fun mergeChecklist(key: String, o: SmokeOut) {
        require(key in COMMON_EVENT_CHECKLIST_SET) { "unknown checklist key: $key" }
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

    fun zhUInt(v: Int): String = when {
        v == ApiGuard.CODE_API_VERSION_TOO_HIGH ->
            "释义：API_VERSION_TOO_HIGH（ApiGuard）— 已拦截，未进入 native；具体说明见本行后附或报告末尾「API 版本不符」明细。"
        v == COMMONEVENT_ERR_OK.toInt() ->
            "释义：0 即 COMMONEVENT_ERR_OK，表示执行成功（CommonEvent_ErrCode / capi-oh-commonevent-h）。"
        v == COMMONEVENT_ERR_PERMISSION_ERROR.toInt() ->
            "释义：201 COMMONEVENT_ERR_PERMISSION_ERROR，权限校验失败。"
        v == COMMONEVENT_ERR_INVALID_PARAMETER.toInt() ->
            "释义：401 COMMONEVENT_ERR_INVALID_PARAMETER，参数无效。"
        v == COMMONEVENT_ERR_SENDING_LIMIT_EXCEEDED.toInt() ->
            "释义：1500003 发送频率过高。"
        v == COMMONEVENT_ERR_NOT_SYSTEM_SERVICE.toInt() ->
            "释义：1500004 非系统服务发送系统事件。"
        v == COMMONEVENT_ERR_SENDING_REQUEST_FAILED.toInt() ->
            "释义：1500007 IPC 发送失败。"
        v == COMMONEVENT_ERR_INIT_UNDONE.toInt() ->
            "释义：1500008 公共事件服务未初始化完成。"
        v == COMMONEVENT_ERR_OBTAIN_SYSTEM_PARAMS.toInt() ->
            "释义：1500009 获取系统参数失败。"
        v == COMMONEVENT_ERR_SUBSCRIBER_NUM_EXCEEDED.toInt() ->
            "释义：1500010 订阅者数量超限。"
        v == COMMONEVENT_ERR_ALLOC_MEMORY_FAILED.toInt() ->
            "释义：1500011 内存分配失败。"
        else ->
            "释义：非 0 返回值请对照官方 CommonEvent_ErrCode。"
    }

    fun zhPtr(nonNull: Boolean, what: String): String =
        if (nonNull) {
            "释义：non-null — $what 创建成功，使用后须按文档 Destroy*。"
        } else {
            "释义：null — $what 未创建成功或 API 不可用（ApiGuard 亦可能拦截）。"
        }

    return buildString {
        appendLine("=== OH_CommonEvent 公共事件 CAPI 验证（platform.BasicServicesKit.OH_CommonEvent.*，含中文释义）===")
        appendLine(
            "bundle=$bundleName module=$moduleName event=my.app.test.custom_event" +
                "；SetPublisherBundleName 使用 bundle=$bundleResolved；module=$moduleResolved。",
        )
        appendLine(
            "清单共 23 项（图 39–61）：订阅/发布/销毁及回调内 Get 与 HasKey。官方：capi-oh-commonevent-h。",
        )
        appendLine()

        memScoped {
            val bundle = bundleResolved
            val eventName = "my.app.test.custom_event"

            fun record(
                name: String,
                raw: String,
                o: SmokeOut,
                extra: String? = null,
                statKey: String? = null,
                omitFailDetail: Boolean = false,
            ) {
                detailLineCount++
                val key = statKey ?: ceStatKeyFromLabel(name)
                if (key != null) {
                    mergeChecklist(key, o)
                }
                appendLine("$name: $raw")
                appendLine("  → ${extra ?: ""}")
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

            fun gUInt(
                name: String,
                statKey: String? = null,
                omitFailDetail: Boolean = false,
                block: () -> UInt,
            ) {
                val v = ApiGuard.guardInt { block().toInt() }
                val verHint = ApiGuard.takeVersionHint()
                val raw = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) "API_VERSION_TOO_HIGH" else "$v"
                val o = when {
                    v == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                    v == COMMONEVENT_ERR_OK.toInt() -> SmokeOut.Ok
                    else -> SmokeOut.Fail
                }
                val extra = buildString {
                    append(zhUInt(v))
                    if (!verHint.isNullOrBlank()) {
                        append(" ")
                        append(verHint)
                    }
                }
                record(name, raw, o, extra, statKey, omitFailDetail)
            }

            fun gInvoke(name: String, statKey: String? = null, block: () -> Unit) {
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
                            ApiGuard.CODE_API_VERSION_TOO_HIGH -> zhUInt(v)
                            0 -> "释义：0 — guardInvoke 成功（void 返回类 API，无 CommonEvent_ErrCode）。"
                            -1 -> "释义：-1 — guardInvoke 捕获非版本类异常，native 未正常完成。"
                            else -> zhUInt(v)
                        },
                    )
                    if (!verHint.isNullOrBlank()) {
                        append(" ")
                        append(verHint)
                    }
                }
                record(name, raw, o, extra, statKey)
            }

            fun gPtr(name: String, nonNull: Boolean, what: String, statKey: String) {
                val o = if (nonNull) SmokeOut.Ok else SmokeOut.Ver
                val raw = if (nonNull) "non-null" else "null"
                val extra = zhPtr(nonNull, what) + " ${ApiGuard.takeVersionHint()?.let { " $it" } ?: ""}"
                record(name, raw, o, extra.trim(), statKey)
            }

            val eventPtrSlots = allocArrayOf(eventName.cstr.ptr)

            var info: CPointer<CommonEvent_SubscribeInfo>? = null
            info = ApiGuard.guard({ OH_CommonEvent_CreateSubscribeInfo(eventPtrSlots, 1) }, null, null)
            gPtr(
                "OH_CommonEvent_CreateSubscribeInfo(events=[\"$eventName\"], eventsNum=1)",
                info != null,
                "CommonEvent_SubscribeInfo",
                "OH_CommonEvent_CreateSubscribeInfo",
            )

            if (info != null) {
                gUInt(
                    "OH_CommonEvent_SetPublisherPermission(info, permission=\"ohos.permission.GET_BUNDLE_INFO\")",
                    "OH_CommonEvent_SetPublisherPermission",
                ) {
                    OH_CommonEvent_SetPublisherPermission(info, "ohos.permission.GET_BUNDLE_INFO")
                }

                gUInt(
                    "OH_CommonEvent_SetPublisherBundleName(info, bundleName=\"$bundle\")",
                    "OH_CommonEvent_SetPublisherBundleName",
                ) { OH_CommonEvent_SetPublisherBundleName(info, bundle) }

                var sub: COpaquePointer? = null
                sub = ApiGuard.guard(
                    { OH_CommonEvent_CreateSubscriber(info, commonEventReceiveCallback) },
                    null,
                    null,
                )
                gPtr(
                    "OH_CommonEvent_CreateSubscriber(info, callback=commonEventReceiveCallback)",
                    sub != null,
                    "CommonEvent_Subscriber（平台绑定为不透明指针）",
                    "OH_CommonEvent_CreateSubscriber",
                )

                if (sub != null) {
                    gUInt("OH_CommonEvent_Subscribe(subscriber=non-null)") { OH_CommonEvent_Subscribe(sub) }

                    gUInt("OH_CommonEvent_Publish(event=\"$eventName\")") { OH_CommonEvent_Publish(eventName) }
                    usleep(400_000u)
                    appendLine("  → 释义：首次 Publish 后延迟 400ms，再展开回调内合并的记录。")
                    val nFlush1 = flushCePendingRecords { n, r, o, e, s -> record(n, r, o, e, s) }
                    appendLine("  → 释义：本轮合并 $nFlush1 条回调内记录。")

                    var params: COpaquePointer? = null
                    var pubInfo: CPointer<CommonEvent_PublishInfo>? = null

                    params = ApiGuard.guard({ OH_CommonEvent_CreateParameters() }, null, null)
                    gPtr(
                        "OH_CommonEvent_CreateParameters()",
                        params != null,
                        "CommonEvent_Parameters",
                        "OH_CommonEvent_CreateParameters",
                    )

                    if (params != null) {
                        gUInt(
                            "OH_CommonEvent_SetDoubleToParameters(param, key=\"dKey\", value=1.5)",
                            "OH_CommonEvent_SetDoubleToParameters",
                        ) { OH_CommonEvent_SetDoubleToParameters(params, "dKey", 1.5) }

                        gUInt(
                            "OH_CommonEvent_SetLongToParameters(param, key=\"longKey\", value=1L)",
                            "OH_CommonEvent_SetLongToParameters",
                        ) { OH_CommonEvent_SetLongToParameters(params, "longKey", 1L) }

                        gUInt(
                            "OH_CommonEvent_SetCharToParameters(param, key=\"cKey\", value='y')",
                            "OH_CommonEvent_SetCharToParameters",
                        ) { OH_CommonEvent_SetCharToParameters(params, "cKey", 'y'.code.toByte()) }

                        // 不在图 39–61 的 23 项内；与图 3 演示一致写入 bool，供回调 GetBoolFromParameters
                        gUInt(
                            "（清单外）OH_CommonEvent_SetBoolToParameters(param, key=\"boolKey\", value=true)",
                            null,
                        ) { OH_CommonEvent_SetBoolToParameters(params, "boolKey", true) }

                        pubInfo = ApiGuard.guard({ OH_CommonEvent_CreatePublishInfo(true) }, null, null)
                        gPtr(
                            "OH_CommonEvent_CreatePublishInfo(ordered=true)",
                            pubInfo != null,
                            "CommonEvent_PublishInfo",
                            "OH_CommonEvent_CreatePublishInfo",
                        )

                        if (pubInfo != null) {
                            gUInt(
                                "OH_CommonEvent_SetPublishInfoParameters(pubInfo, param=parameters)",
                                "OH_CommonEvent_SetPublishInfoParameters",
                            ) { OH_CommonEvent_SetPublishInfoParameters(pubInfo, params) }

                            gUInt(
                                "OH_CommonEvent_PublishWithInfo(event=\"$eventName\", pubInfo=non-null)",
                                "OH_CommonEvent_PublishWithInfo",
                            ) { OH_CommonEvent_PublishWithInfo(eventName, pubInfo) }

                            usleep(400_000u)
                            appendLine(
                                "  → 释义：OH_CommonEvent_PublishWithInfo 后延迟 400ms，再次等待回调（应携带 Parameters）。",
                            )
                            val nFlush2 = flushCePendingRecords { n, r, o, e, s -> record(n, r, o, e, s) }
                            appendLine("  → 释义：PublishWithInfo 后本轮合并 $nFlush2 条回调内记录。")

                            gInvoke(
                                "OH_CommonEvent_DestroyPublishInfo(pubInfo)（清单外释放）",
                                null,
                            ) { OH_CommonEvent_DestroyPublishInfo(pubInfo) }
                        }
                        gInvoke(
                            "OH_CommonEvent_DestroyParameters(param)（清单外释放）",
                            null,
                        ) { OH_CommonEvent_DestroyParameters(params) }
                    }

                    gUInt("OH_CommonEvent_UnSubscribe(subscriber=non-null)") { OH_CommonEvent_UnSubscribe(sub) }
                    gInvoke("OH_CommonEvent_DestroySubscriber(subscriber=non-null)") { OH_CommonEvent_DestroySubscriber(sub) }
                }
                gInvoke("OH_CommonEvent_DestroySubscribeInfo(info=non-null)") { OH_CommonEvent_DestroySubscribeInfo(info) }
            }
        }

        appendLine()

        val fixedBatteryChanged = "usual.event.BATTERY_CHANGED"
        val cinteropBattery = COMMON_EVENT_BATTERY_CHANGED?.toKString().orEmpty()
        val batteryOk = fixedBatteryChanged == cinteropBattery
        val macroLines = listOf(
            Triple(
                "COMMON_EVENT_BATTERY_CHANGED",
                "固定参照 usual.event.BATTERY_CHANGED",
                "cinterop=$cinteropBattery — ${if (batteryOk) "一致" else "不一致"}",
            ),
        )
        appendLine("---------- 常量比对（cinterop vs 固定参照）----------")
        macroLines.forEachIndexed { i, t ->
            appendLine("${i + 1}. ${t.first} — ${t.second} — ${t.third}")
        }
        val macroOkCnt = macroLines.count { it.third.endsWith("一致") }
        appendLine()
        appendLine("宏比对汇总：一致=$macroOkCnt 不一致=${macroLines.size - macroOkCnt}")
        appendLine()

        appendLine("---------- 清单统计（共 ${COMMON_EVENT_CHECKLIST_23.size} 项，图 39–61）----------")
        var ckOk = 0
        var ckFail = 0
        var ckVer = 0
        var ckNotRun = 0
        COMMON_EVENT_CHECKLIST_23.forEachIndexed { index, key ->
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
        appendLine("核对：${ckOk + ckFail + ckVer + ckNotRun} = ${COMMON_EVENT_CHECKLIST_23.size}（应为 23）")
        appendLine()
        // appendLine("明细输出行数：$detailLineCount（含「清单外」步骤；清单外不参与 23 项计数）")
        // appendLine()
        appendLine("---------- 常量列表 ----------")
        macroLines.forEachIndexed { i, t ->
            val ok = (i == 0 && batteryOk)
            appendLine("${i + 1}. ${t.first} — ${t.second} — ${t.third} — ${if (ok) "一致" else "不一致"}")
        }
        appendLine()
        appendLine("宏列表汇总：一致=$macroOkCnt 不一致=${macroLines.size - macroOkCnt}")
        appendLine()
        appendLine("---------- 判定失败明细（含非清单步骤） ----------")
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
        appendLine(
            "  → 释义：「未执行」表示该清单项在报告中从未被合并过判定；" +
                "常见于前置步骤失败、ApiGuard 拦截或分支未走到。",
        )
        val notRunKeys = COMMON_EVENT_CHECKLIST_23.filter { checklistOutcome[it] == null }
        if (notRunKeys.isEmpty()) {
            appendLine("（无）")
        } else {
            notRunKeys.forEach { k ->
                appendLine("• $k")
                appendLine("  → ${ceNotRunHint(k)}")
            }
        }
    }
}

private fun ceNotRunHint(key: String): String = when (key) {
    "OH_CommonEvent_CreateSubscribeInfo",
    "OH_CommonEvent_SetPublisherPermission",
    "OH_CommonEvent_SetPublisherBundleName",
    ->
        "未执行：通常因应用上下文或公共事件服务未就绪，或 CreateSubscribeInfo 已返回 null。"
    "OH_CommonEvent_CreateSubscriber",
    "OH_CommonEvent_Subscribe",
    ->
        "未执行：SubscribeInfo 或 Subscriber 未创建成功。"
    "OH_CommonEvent_Publish",
    "OH_CommonEvent_GetEventFromRcvData",
    "OH_CommonEvent_GetBundleNameFromRcvData",
    "OH_CommonEvent_GetParametersFromRcvData",
    "OH_CommonEvent_HasKeyInParameters",
    "OH_CommonEvent_GetLongFromParameters",
    "OH_CommonEvent_GetDoubleFromParameters",
    "OH_CommonEvent_GetCharFromParameters",
    ->
        "未执行：未进入 Subscribe/Publish 或未 flush 回调记录。"
    "OH_CommonEvent_CreateParameters",
    "OH_CommonEvent_SetDoubleToParameters",
    "OH_CommonEvent_SetLongToParameters",
    "OH_CommonEvent_SetCharToParameters",
    "OH_CommonEvent_CreatePublishInfo",
    "OH_CommonEvent_SetPublishInfoParameters",
    "OH_CommonEvent_PublishWithInfo",
    ->
        "未执行：Parameters/PublishInfo 分支未执行（多为 CreateParameters 为 null）。"
    "OH_CommonEvent_UnSubscribe",
    "OH_CommonEvent_DestroySubscriber",
    "OH_CommonEvent_DestroySubscribeInfo",
    ->
        "未执行：未走到订阅成功后的清理路径。"
    else -> "未执行：请结合上文步骤与设备日志。"
}

@CName("kn_runCommonEventModuleSmokeTest")
fun kn_runCommonEventModuleSmokeTest(
    dbDir: CPointer<ByteVar>?,
    bundleName: CPointer<ByteVar>?,
    moduleName: CPointer<ByteVar>?,
): CPointer<ByteVar>? {
    val dir = dbDir?.toKString().orEmpty()
    val bundle = bundleName?.toKString().orEmpty()
    val module = moduleName?.toKString().orEmpty()
    val report = try {
        buildCommonEventModuleSmokeReport(dir, bundle, module)
    } catch (t: Throwable) {
        "Kotlin 异常: ${t::class.simpleName}: ${t.message}\n${t.stackTraceToString()}"
    }
    return strdup(report)
}
