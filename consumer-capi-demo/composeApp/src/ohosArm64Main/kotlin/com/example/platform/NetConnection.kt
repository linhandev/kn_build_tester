@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.experimental.ExperimentalNativeApi::class,
)

package com.example.platform.NetConnection

import com.example.test.common.ApiGuard
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName
import kotlinx.cinterop.*
import platform.NetworkKit.NetConnection.NETCONN_INTERNAL_ERROR
import platform.NetworkKit.NetConnection.NETCONN_OPERATION_FAILED
import platform.NetworkKit.NetConnection.NETCONN_PARAMETER_ERROR
import platform.NetworkKit.NetConnection.NETCONN_PERMISSION_DENIED
import platform.NetworkKit.NetConnection.NETCONN_SUCCESS
import platform.NetworkKit.NetConnection.NetConn_ConnectionProperties
import platform.NetworkKit.NetConnection.NetConn_HttpProxy
import platform.NetworkKit.NetConnection.NetConn_NetCapabilities
import platform.NetworkKit.NetConnection.NetConn_NetConnCallback
import platform.NetworkKit.NetConnection.NetConn_NetHandle
import platform.NetworkKit.NetConnection.NetConn_ProbeResultInfo
import platform.NetworkKit.NetConnection.OH_NetConn_GetDefaultHttpProxy
import platform.NetworkKit.NetConnection.OH_NetConn_QueryProbeResult
import platform.NetworkKit.NetConnection.OH_NetConn_RegisterDefaultNetConnCallback
import platform.NetworkKit.NetConnection.OH_NetConn_SetAppHttpProxy
import platform.NetworkKit.NetConnection.OH_NetConn_UnregisterNetConnCallback
import platform.posix.memcpy
import platform.posix.memset
import platform.posix.strdup
import platform.posix.usleep

/**
 * NetConnection CAPI（场景一：默认 HTTP 代理 / 网络探测(QueryProbeResult) / 应用代理；场景二：默认网络回调注册与注销）。
 *
 * API：[capi-net-connection-h](https://developer.huawei.com/consumer/cn/doc/harmonyos-references/capi-net-connection-h)，头文件 `net_connection.h` / `net_connection_type.h`。
 */

private enum class SmokeOut { Ok, Fail, Ver }

/** 与需求图一致至少 5 个函数。 */
private val NETCONN_CHECKLIST_5 = listOf(
    "OH_NetConn_GetDefaultHttpProxy",
    "OH_NetConn_QueryProbeResult",
    "OH_NetConn_SetAppHttpProxy",
    "OH_NetConn_RegisterDefaultNetConnCallback",
    "OH_NetConn_UnregisterNetConnCallback",
)

private val NETCONN_CHECKLIST_SET = NETCONN_CHECKLIST_5.toSet()

private fun netStatKeyFromLabel(label: String): String? =
    label.substringBefore("(").trim().takeIf { it in NETCONN_CHECKLIST_SET }

private const val PROBE_DEST_BUF_LEN = 256

/** 探测时长（秒），头文件注明 duration 单位为秒。 */
private const val PROBE_DURATION_SEC = 3

/** 回调日志（staticCFunction 写入，主流程合并进报告）。 */
private val netConnCallbackLines = mutableListOf<String>()

private fun netConnCallbackAppend(line: String) {
    netConnCallbackLines.add(line)
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
private val netOnNetworkAvailable =
    staticCFunction { h: CPointer<NetConn_NetHandle>? ->
        if (h == null) {
            netConnCallbackAppend("onNetworkAvailable(handle=null)")
        } else {
            val id = h.pointed.netId
            netConnCallbackAppend(
                "[回调→页面] RegisterDefaultNetConnCallback：onNetworkAvailable 已触发 netId=$id",
            )
            netConnCallbackAppend("onNetworkAvailable(netId=$id)")
        }
    }

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
private val netOnNetCapabilitiesChange =
    staticCFunction { h: CPointer<NetConn_NetHandle>?, _: CPointer<NetConn_NetCapabilities>? ->
        val id = h?.pointed?.netId
        netConnCallbackAppend("onNetCapabilitiesChange(netId=${id ?: "n/a"})")
    }

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
private val netOnConnectionPropertiesChange =
    staticCFunction { h: CPointer<NetConn_NetHandle>?, _: CPointer<NetConn_ConnectionProperties>? ->
        val id = h?.pointed?.netId
        netConnCallbackAppend("onConnetionProperties(netId=${id ?: "n/a"})")
    }

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
private val netOnNetLost =
    staticCFunction { h: CPointer<NetConn_NetHandle>? ->
        val id = h?.pointed?.netId
        netConnCallbackAppend("onNetLost(netId=${id ?: "n/a"})")
    }

private fun netOnNetUnavailableImpl() {
    netConnCallbackAppend("onNetUnavailable")
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
private val netOnNetUnavailable = staticCFunction(::netOnNetUnavailableImpl)

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
private val netOnNetBlockStatusChange =
    staticCFunction { h: CPointer<NetConn_NetHandle>?, blocked: Boolean ->
        val id = h?.pointed?.netId
        netConnCallbackAppend("onNetBlockStatusChange(netId=${id ?: "n/a"}, blocked=$blocked)")
    }

@Suppress("MagicNumber", "LongMethod", "CognitiveComplexity")
fun buildNetConnectionModuleSmokeReport(
    @Suppress("UNUSED_PARAMETER") databaseDir: String,
    bundleName: String,
    moduleName: String,
): String {
    val failDetails = mutableListOf<String>()
    val verDetails = mutableListOf<String>()
    val checklistOutcome = mutableMapOf<String, SmokeOut>()
    var detailLineCount = 0

    fun mergeChecklist(key: String, o: SmokeOut) {
        require(key in NETCONN_CHECKLIST_SET) { "unknown checklist key: $key" }
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

    fun zhInt32(code: Int): String = when {
        code == ApiGuard.CODE_API_VERSION_TOO_HIGH ->
            "释义：API_VERSION_TOO_HIGH（ApiGuard），未进入 native；见报告末尾「API 版本不符」明细。"
        code == 0 ->
            "释义：0 表示成功（int32_t 返回类接口）。"
        code == 201 ->
            "释义：201 权限不足（如缺少 ohos.permission.GET_NETWORK_INFO）。"
        code == 401 ->
            "释义：401 参数错误。"
        code == 2100002 ->
            "释义：2100002 连接服务失败。"
        code == 2100003 ->
            "释义：2100003 系统内部错误。"
        else ->
            "释义：非 0 请对照 NetConnection 错误码说明。"
    }

    fun zhNetConnErrU(code: Int): String = when {
        code == ApiGuard.CODE_API_VERSION_TOO_HIGH ->
            "释义：API_VERSION_TOO_HIGH（ApiGuard）。"
        code == NETCONN_SUCCESS.toInt() ->
            "释义：0（NETCONN_SUCCESS），探测调用成功。"
        code == NETCONN_PERMISSION_DENIED.toInt() ->
            "释义：NETCONN_PERMISSION_DENIED(201)。"
        code == NETCONN_PARAMETER_ERROR.toInt() ->
            "释义：NETCONN_PARAMETER_ERROR(401)，缓冲区或参数无效。"
        code == NETCONN_OPERATION_FAILED.toInt() ->
            "释义：NETCONN_OPERATION_FAILED(2100002)。"
        code == NETCONN_INTERNAL_ERROR.toInt() ->
            "释义：NETCONN_INTERNAL_ERROR(2100003)。"
        else ->
            "释义：请对照文档 int32_t / NetConn_ErrorCode（QueryProbeResult 等）。"
    }

    return buildString {
        appendLine("=== NetConnection 网络连接 CAPI 验证（platform.NetworkKit.NetConnection.*）===")
        appendLine("bundle=$bundleName module=$moduleName")
        appendLine(
            "清单 5 项：GetDefaultHttpProxy → QueryProbeResult(dest,duration,outNetConn_ProbeResultInfo) → " +
                "SetAppHttpProxy → RegisterDefaultNetConnCallback → UnregisterNetConnCallback。",
        )
        appendLine(
            "说明：QueryProbeResult @since 20，需 ohos.permission.INTERNET；对目标 IP 做短时探测并回填 lossRate / rtt[4]（μs）。",
        )
        appendLine(
            "文档：[capi-net-connection-h](https://developer.huawei.com/consumer/cn/doc/harmonyos-references/capi-net-connection-h)。",
        )
        appendLine()

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
                detailLineCount++
                val key = statKey ?: netStatKeyFromLabel(name)
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

            fun gInt32(name: String, statKey: String, block: () -> Int) {
                val v = ApiGuard.guardInt(block)
                val verHint = ApiGuard.takeVersionHint()
                val raw = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) "API_VERSION_TOO_HIGH" else "$v"
                val o = when {
                    v == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                    v == 0 -> SmokeOut.Ok
                    else -> SmokeOut.Fail
                }
                val extra = buildString {
                    append(zhInt32(v))
                    if (!verHint.isNullOrBlank()) {
                        append(" ")
                        append(verHint)
                    }
                }
                record(name, raw, o, extra, statKey)
            }

            fun gQueryProbeResult(
                name: String,
                statKey: String,
                probeInfo: CPointer<NetConn_ProbeResultInfo>,
                block: () -> Int,
            ) {
                val v = ApiGuard.guardInt(block)
                val verHint = ApiGuard.takeVersionHint()
                val raw = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) "API_VERSION_TOO_HIGH" else "$v"
                val o = when {
                    v == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                    v == NETCONN_SUCCESS.toInt() -> SmokeOut.Ok
                    else -> SmokeOut.Fail
                }
                val pr = probeInfo.pointed
                val rttStr = buildString {
                    append('[')
                    for (i in 0 until 4) {
                        if (i > 0) append(',')
                        append(pr.rtt[i])
                    }
                    append(']')
                }
                val extra = buildString {
                    append(zhNetConnErrU(v))
                    append(" 输出结构体：lossRate=${pr.lossRate}，rtt(μs)=$rttStr。")
                    if (v == NETCONN_INTERNAL_ERROR.toInt()) {
                        append(
                            " 失败说明：文档标注 2100003 为内部错误；" +
                                "探测场景下多见于无可用网络、目标不可达、探测超时或服务端异常，可检查联网与 INTERNET 权限后重试。",
                        )
                    }
                    if (!verHint.isNullOrBlank()) {
                        append(" ")
                        append(verHint)
                    }
                }
                record(name, raw, o, extra, statKey)
            }

            fun fillHttpProxyForSet(p: NetConn_HttpProxy) {
                memset(p.ptr, 0, sizeOf<NetConn_HttpProxy>().toULong())
                val host = "192.168.0.1"
                host.encodeToByteArray().usePinned { pin ->
                    memcpy(p.ptr.reinterpret<ByteVar>(), pin.addressOf(0), host.length.toULong())
                }
                p.port = 8888u
                p.exclusionListSize = 0
            }

            val proxyRead = alloc<NetConn_HttpProxy>()
            memset(proxyRead.ptr, 0, sizeOf<NetConn_HttpProxy>().toULong())

            gInt32(
                "OH_NetConn_GetDefaultHttpProxy(httpProxy=out)",
                "OH_NetConn_GetDefaultHttpProxy",
            ) {
                OH_NetConn_GetDefaultHttpProxy(proxyRead.ptr)
            }

            val destHost = "223.5.5.5"
            val destBuf = allocArray<ByteVar>(PROBE_DEST_BUF_LEN)
            memset(destBuf, 0, PROBE_DEST_BUF_LEN.toULong())
            destHost.encodeToByteArray().usePinned { pin ->
                val n = destHost.length
                require(n + 1 <= PROBE_DEST_BUF_LEN)
                memcpy(destBuf, pin.addressOf(0), n.toULong())
                destBuf[n] = 0
            }
            val probeInfo = alloc<NetConn_ProbeResultInfo>()
            memset(probeInfo.ptr, 0, sizeOf<NetConn_ProbeResultInfo>().toULong())

            gQueryProbeResult(
                "OH_NetConn_QueryProbeResult(destination=\"$destHost\",duration=${PROBE_DURATION_SEC}s," +
                    "probeResultInfo=out)",
                "OH_NetConn_QueryProbeResult",
                probeInfo.ptr,
            ) {
                OH_NetConn_QueryProbeResult(destBuf, PROBE_DURATION_SEC, probeInfo.ptr)
            }

            val proxySet = alloc<NetConn_HttpProxy>()
            fillHttpProxyForSet(proxySet)

            gInt32(
                "OH_NetConn_SetAppHttpProxy(httpProxy=host=192.168.0.1,port=8888,exclusionListSize=0)",
                "OH_NetConn_SetAppHttpProxy",
            ) {
                OH_NetConn_SetAppHttpProxy(proxySet.ptr)
            }

            netConnCallbackLines.clear()
            val cb = alloc<NetConn_NetConnCallback>()
            cb.onNetworkAvailable = netOnNetworkAvailable
            cb.onNetCapabilitiesChange = netOnNetCapabilitiesChange
            cb.onConnetionProperties = netOnConnectionPropertiesChange
            cb.onNetLost = netOnNetLost
            cb.onNetUnavailable = netOnNetUnavailable
            cb.onNetBlockStatusChange = netOnNetBlockStatusChange

            val callbackIdVar = alloc<UIntVar>()
            callbackIdVar.value = 0u

            gInt32(
                "OH_NetConn_RegisterDefaultNetConnCallback(callback=六槽位 staticCFunction, callbackId=out)",
                "OH_NetConn_RegisterDefaultNetConnCallback",
            ) {
                OH_NetConn_RegisterDefaultNetConnCallback(cb.ptr, callbackIdVar.ptr)
            }

            usleep(500_000u)
            sb.appendLine("---------- 默认网络回调（Register 后延迟 500ms 采样）----------")
            if (netConnCallbackLines.isEmpty()) {
                sb.appendLine("（本时段无槽位输出：500ms 内可能无网络事件）")
            } else {
                netConnCallbackLines.forEach { sb.appendLine(it) }
            }
            val hasPageProbe = netConnCallbackLines.any { it.startsWith("[回调→页面]") }
            val cbVerifyOk = netConnCallbackLines.isNotEmpty() && hasPageProbe
            detailLineCount += 2
            sb.appendLine(
                "[回调验证] OH_NetConn_RegisterDefaultNetConnCallback：${if (cbVerifyOk) "成功" else "未确认"}",
            )
            sb.appendLine(
                "  → 释义：${if (cbVerifyOk) {
                    "报告上已出现以 [回调→页面] 开头的行，表明传给 Register 的结构体内函数指针已被 native 调用（与 CommonEvent 回调探针同理）。"
                } else {
                    "采样窗口内无 [回调→页面] 行或无输出，无法在本页确认槽位已触发；可略增大延迟或检查网络状态。"
                }}",
            )
            val registerKey = "OH_NetConn_RegisterDefaultNetConnCallback"
            val registerOk = checklistOutcome[registerKey] == SmokeOut.Ok
            when {
                cbVerifyOk || !registerOk -> Unit
                netConnCallbackLines.isEmpty() ->
                    failDetails.add("• [回调验证]：Register 已成功但 500ms 内无回调输出。")
                else ->
                    failDetails.add("• [回调验证]：Register 已成功但有输出却缺少 [回调→页面] 探针行。")
            }
            sb.appendLine()

            val cid = callbackIdVar.value
            gInt32(
                "OH_NetConn_UnregisterNetConnCallback(callbackId=$cid)",
                "OH_NetConn_UnregisterNetConnCallback",
            ) {
                OH_NetConn_UnregisterNetConnCallback(cid)
            }

            sb.toString()
        }

        append(body)

        appendLine()
        appendLine("---------- 清单统计（共 ${NETCONN_CHECKLIST_5.size} 项）----------")
        var ckOk = 0
        var ckFail = 0
        var ckVer = 0
        var ckNotRun = 0
        NETCONN_CHECKLIST_5.forEachIndexed { index, key ->
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
        appendLine("核对：${ckOk + ckFail + ckVer + ckNotRun} = ${NETCONN_CHECKLIST_5.size}（应为 5）")
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
        val notRunKeys = NETCONN_CHECKLIST_5.filter { checklistOutcome[it] == null }
        if (notRunKeys.isEmpty()) {
            appendLine("（无）")
        } else {
            notRunKeys.forEach { k ->
                appendLine("• $k — 前置失败或未合并 record。")
            }
        }
    }
}

@CName("kn_runNetConnectionModuleSmokeTest")
fun kn_runNetConnectionModuleSmokeTest(
    dbDir: CPointer<ByteVar>?,
    bundleName: CPointer<ByteVar>?,
    moduleName: CPointer<ByteVar>?,
): CPointer<ByteVar>? {
    val dir = dbDir?.toKString().orEmpty()
    val bundle = bundleName?.toKString().orEmpty()
    val module = moduleName?.toKString().orEmpty()
    val report = try {
        buildNetConnectionModuleSmokeReport(dir, bundle, module)
    } catch (t: Throwable) {
        "Kotlin 异常: ${t::class.simpleName}: ${t.message}\n${t.stackTraceToString()}"
    }
    return strdup(report)
}
