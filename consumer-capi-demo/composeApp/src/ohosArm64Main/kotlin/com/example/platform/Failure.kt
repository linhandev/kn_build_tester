@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.experimental.ExperimentalNativeApi::class,
)

package com.example.platform.Failure

import com.example.test.common.ApiGuard
import kotlinx.cinterop.*
import kotlin.native.CName
import platform.ArkData.RDB.OH_Rdb_CreateConfig
import platform.ArkData.RDB.OH_Rdb_DestroyConfig
import platform.ArkData.RDB.OH_Rdb_SetDatabaseDir
import platform.ArkData.RDB.OH_Rdb_CreateOrOpen
import platform.ArkData.RDB.OH_Rdb_SetStoreName
import platform.BasicServicesKit.OH_CommonEvent.OH_CommonEvent_CreateSubscribeInfo
import platform.BasicServicesKit.OH_CommonEvent.OH_CommonEvent_Subscribe
import platform.BasicServicesKit.OH_CommonEvent.OH_CommonEvent_SetPublisherPermission
import platform.NetworkKit.NetConnection.NetConn_HttpProxy
import platform.NetworkKit.NetConnection.OH_NetConn_UnregisterNetConnCallback
import platform.PerformanceAnalysisKit.HiAppEvent.OH_HiAppEvent_Write
import platform.PerformanceAnalysisKit.HiLog.LOG_APP
import platform.PerformanceAnalysisKit.HiLog.LOG_DEBUG
import platform.PerformanceAnalysisKit.HiLog.OH_LOG_Print
import platform.UniversalKeystoreKit.HuksKeyApi.OH_Huks_GetSdkVersion
import platform.UniversalKeystoreKit.HuksTypeApi.OH_Huks_Blob
import platform.ArkGraphics2D.Drawing.OH_Drawing_GetFontCollectionGlobalInstance
import platform.ArkGraphics2D.Drawing.OH_Drawing_RegisterFont
import platform.posix.strdup
import platform.posix.memset

private fun zhFailHint(module: String, reason: String): String =
    "释义：为构造失败场景，向 $module 选定函数传入空值/边界/异常参数，期望返回非成功码。$reason"

private enum class SmokeOut { Ok, Fail, Ver }

@Suppress("MagicNumber")
fun buildFailureModuleSmokeReport(
    databaseDir: String,
    bundleName: String,
    moduleName: String,
): String = buildString {
    appendLine("=== 失败场景验证（各模块挑 1 个函数，使用空值/边界值/异常值构造失败返回）===")
    appendLine("dbDir=$databaseDir bundle=$bundleName module=$moduleName")
    appendLine()

    memScoped {        
        val failDetails = mutableListOf<String>()
        val verDetails = mutableListOf<String>()
        val checklistOutcome = mutableMapOf<String, SmokeOut>()
        val CHECKLIST = listOf(
            "OH_Rdb_CreateOrOpen",
            "OH_CommonEvent_Subscribe",
            "OH_Huks_GetSdkVersion",
            "OH_NetConn_UnregisterNetConnCallback",
            "OH_HiAppEvent_Write",
            "OH_LOG_Print",
            "OH_Drawing_RegisterFont",
        )
        fun mergeChecklist(key: String, o: SmokeOut) {
            fun rank(x: SmokeOut) = when (x) { SmokeOut.Ok -> 0; SmokeOut.Fail -> 1; SmokeOut.Ver -> 2 }
            checklistOutcome[key] = checklistOutcome[key]?.let { if (rank(o) >= rank(it)) o else it } ?: o
        }
        fun record(name: String, raw: String, o: SmokeOut, extra: String, statKey: String) {
            appendLine("$name: $raw")
            appendLine("  → $extra")
            mergeChecklist(statKey, o)
            when (o) {
                SmokeOut.Fail -> failDetails.add("• $name：结果 $raw。$extra")
                SmokeOut.Ver -> verDetails.add("• $name：结果 $raw。$extra")
                else -> {}
            }
        }
        fun zhRdbInt(v: Int): String =
            when {
                v == 0 ->
                    "释义：0（RDB_OK）— 执行成功。"
                v == -1 ->
                    "释义：-1 — 通用失败/异常占位，需结合日志定位。"
                v == 14_800_001 ->
                    "释义：14800001（RDB_E_INVALID_ARGS）— 参数无效（如目录/库名/上下文不合法）。"
                v in 14_800_000..14_899_999 ->
                    "释义：148xxxxx（RDB 业务错误码区间）— 具体含义见 OH_Rdb_ErrCode。"
                else ->
                    "释义：非常见码，参考 OH_Rdb_ErrCode 对照表。"
            }
        fun zhCEUInt(v: Int): String =
            when (v) {
                0 ->
                    "释义：0（COMMONEVENT_ERR_OK）— 成功。"
                201 ->
                    "释义：201（COMMONEVENT_ERR_PERMISSION_ERROR）— 权限校验失败。"
                401 ->
                    "释义：401（COMMONEVENT_ERR_INVALID_PARAMETER）— 参数非法或为空。"
                1500003 ->
                    "释义：1500003（SENDING_LIMIT_EXCEEDED）— 发送频率过高。"
                1500004 ->
                    "释义：1500004（NOT_SYSTEM_SERVICE）— 非系统服务发送系统事件。"
                1500007 ->
                    "释义：1500007（SENDING_REQUEST_FAILED）— IPC 发送失败。"
                1500008 ->
                    "释义：1500008（INIT_UNDONE）— 服务未初始化完成。"
                else ->
                    "释义：非常见码，参考 CommonEvent_ErrCode 对照表。"
            }
        fun zhHuksErr(v: Int): String =
            when (v) {
                0 ->
                    "释义：0（OH_HUKS_SUCCESS）— 成功。"
                201 ->
                    "释义：201（OH_HUKS_ERR_CODE_PERMISSION_FAIL）— 权限失败。"
                401 ->
                    "释义：401（OH_HUKS_ERR_CODE_ILLEGAL_ARGUMENT）— 参数非法或为空。"
                801 ->
                    "释义：801（OH_HUKS_ERR_CODE_NOT_SUPPORTED_API）— 当前环境不支持该 API。"
                in 12_000_001..12_000_025 ->
                    "释义：12xxxxxx（HUKS 业务错误码区间）— 参考 OH_Huks_ErrCode。"
                else ->
                    "释义：非常见码，参考 OH_Huks_ErrCode 对照表。"
            }
        fun zhNetInt(v: Int): String =
            when (v) {
                0 ->
                    "释义：0 — 成功。"
                201 ->
                    "释义：201 — 缺少权限。"
                401 ->
                    "释义：401 — 参数错误。"
                2_100_002 ->
                    "释义：2100002 — 无法连接到服务。"
                2_100_003 ->
                    "释义：2100003 — 内部错误。"
                2_101_007 ->
                    "释义：2101007 — 回调不存在。"
                else ->
                    "释义：非常见码，参考 NetConnection 错误码说明。"
            }
        fun zhHiAppWrite(v: Int): String = when {
            v == 0 -> "释义：0 — 校验成功并已写入。"
            v > 0 -> "释义：正数 — 存在无效参数被忽略仍写入。"
            v == -1 -> "释义：-1 — 事件名无效。"
            v == -4 -> "释义：-4 — 事件 domain 无效。"
            v == -99 -> "释义：-99 — 功能关闭。"
            else -> "释义：负数 — 校验失败未写入。"
        }
        fun zhHiLogInt(v: Int): String = when {
            v >= 0 -> "释义：≥0 表示本次调用被 HiLog 接受。"
            else -> "释义：<0 表示失败（capi-log-h）。"
        }
        fun zhDrawingUInt(v: UInt): String = if (v == 0u) "释义：0 表示注册成功。" else "释义：非 0 表示注册失败。"

        run {
            val cfg = ApiGuard.guard({ OH_Rdb_CreateConfig() }, null, null)
            if (cfg != null) {
                ApiGuard.guardInt { OH_Rdb_SetDatabaseDir(cfg, "") }
                ApiGuard.guardInt { OH_Rdb_SetStoreName(cfg, "") }
                val err = alloc<IntVar>()
                val store = ApiGuard.guard({ OH_Rdb_CreateOrOpen(cfg, err.ptr) }, null, null)
                val hint = ApiGuard.takeVersionHint()
                val raw = if (!hint.isNullOrBlank()) "API_VERSION_TOO_HIGH" else "store=${if (store == null) "null" else "non-null"} errCode=${err.value}"
                val o = when {
                    !hint.isNullOrBlank() -> SmokeOut.Ver
                    store == null || err.value != 0 -> SmokeOut.Fail
                    else -> SmokeOut.Ok
                }
                val extra = if (!hint.isNullOrBlank()) hint else zhRdbInt(err.value) + "；" + zhFailHint("RDB", "空目录+空库名组合更易触发失败。")
                record("RDB: OH_Rdb_CreateOrOpen(cfg,error=out)", raw, o, extra, "OH_Rdb_CreateOrOpen")
            } else {
                record("RDB: OH_Rdb_CreateConfig()", "null", SmokeOut.Fail, "释义：创建配置失败。", "OH_Rdb_CreateOrOpen")
            }
            if (cfg != null) {
                ApiGuard.guardInvoke { OH_Rdb_DestroyConfig(cfg) }
            }
            appendLine()
        }

        run {
            val v = ApiGuard.guardInt { OH_CommonEvent_Subscribe(null).toInt() }
            val hint = ApiGuard.takeVersionHint()
            val raw = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) "API_VERSION_TOO_HIGH" else "$v"
            val o = when {
                v == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                v == 0 -> SmokeOut.Ok
                else -> SmokeOut.Fail
            }
            val extra = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) (hint ?: "") else zhCEUInt(v)
            record("CommonEvent: OH_CommonEvent_Subscribe(subscriber=null)", raw, o, extra, "OH_CommonEvent_Subscribe")
            appendLine()
        }

        run {
            val v = run {
                val blob = alloc<OH_Huks_Blob>()
                blob.size = 0u
                blob.data = null
                ApiGuard.guardInt { OH_Huks_GetSdkVersion(blob.ptr).useContents { errorCode } }
            }
            val hint = ApiGuard.takeVersionHint()
            val raw = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) "API_VERSION_TOO_HIGH" else "$v"
            val o = when {
                v == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                v == 0 -> SmokeOut.Ok
                else -> SmokeOut.Fail
            }
            val extra = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) (hint ?: "") else zhHuksErr(v)
            record("HuksKeyApi: OH_Huks_GetSdkVersion(size=0,data=null)", raw, o, extra, "OH_Huks_GetSdkVersion")
            appendLine()
        }

        run {
            val v = ApiGuard.guardInt { OH_NetConn_UnregisterNetConnCallback(0u) }
            val hint = ApiGuard.takeVersionHint()
            val raw = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) "API_VERSION_TOO_HIGH" else "$v"
            val o = when {
                v == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                v == 0 -> SmokeOut.Ok
                else -> SmokeOut.Fail
            }
            val extra = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) (hint ?: "") else zhNetInt(v)
            record("NetConnection: OH_NetConn_UnregisterNetConnCallback(callbackId=0)", raw, o, extra, "OH_NetConn_UnregisterNetConnCallback")
            appendLine()
        }

        run {
            val v = ApiGuard.guardInt { OH_HiAppEvent_Write("", "", 1u, null) }
            val hint = ApiGuard.takeVersionHint()
            val raw = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) "API_VERSION_TOO_HIGH" else "$v"
            val o = when {
                v == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                v == 0 || v > 0 -> SmokeOut.Ok
                else -> SmokeOut.Fail
            }
            val extra = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) (hint ?: "") else zhHiAppWrite(v)
            record("HiAppEvent: OH_HiAppEvent_Write(\"\",\"\",FAULT, null)", raw, o, extra, "OH_HiAppEvent_Write")
            appendLine()
        }

        run {
            val longTag = buildString { repeat(2048) { append('T') } }
            val v = ApiGuard.guardInt {
                platform.PerformanceAnalysisKit.HiLog.OH_LOG_Print(
                    LOG_APP,
                    LOG_DEBUG,
                    0x1_0000u, // 超出 0xFFFF 的边界值
                    longTag,
                    "%{public}d", // 少传可变参以制造边界条件
                )
            }
            val hint = ApiGuard.takeVersionHint()
            val raw = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) "API_VERSION_TOO_HIGH" else "$v"
            val o = when {
                v == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                v >= 0 -> SmokeOut.Ok
                else -> SmokeOut.Fail
            }
            val extra = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) (hint ?: "") else "释义：≥0 表示被 HiLog 接受；<0 表示失败。边界构造：domain=0x10000、tag 长度=2048、fmt 缺少变参。"
            record("HiLog: OH_LOG_Print(LOG_APP,LOG_DEBUG,domain=0x10000, tag=2048 chars, fmt=\"%{public}d\")", raw, o, extra, "OH_LOG_Print")
            appendLine()
        }

        run {
            val fc = ApiGuard.guard({ OH_Drawing_GetFontCollectionGlobalInstance() }, null, null)
            ApiGuard.takeVersionHint()
            if (fc != null) {
                val u = ApiGuard.guardInt { OH_Drawing_RegisterFont(fc, "NoSuchFamily", "/path/not/exist.ttf").toInt() }
                val hint = ApiGuard.takeVersionHint()
                val raw = if (u == ApiGuard.CODE_API_VERSION_TOO_HIGH) "API_VERSION_TOO_HIGH" else "$u"
                val o = when {
                    u == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                    u == 0 -> SmokeOut.Ok
                    else -> SmokeOut.Fail
                }
                val extra = if (u == ApiGuard.CODE_API_VERSION_TOO_HIGH) (hint ?: "") else zhDrawingUInt(u.toUInt())
                record("Drawing: OH_Drawing_RegisterFont(\"NoSuchFamily\",\"/path/not/exist.ttf\")", raw, o, extra, "OH_Drawing_RegisterFont")
            } else {
                record("Drawing: OH_Drawing_GetFontCollectionGlobalInstance()", "null", SmokeOut.Fail, "释义：未获取到 FontCollection。", "OH_Drawing_RegisterFont")
            }
            appendLine()
        }

        appendLine("---------- 清单统计（共 ${CHECKLIST.size} 项）----------")
        var ckOk = 0
        var ckFail = 0
        var ckVer = 0
        var ckNotRun = 0
        CHECKLIST.forEachIndexed { index, key ->
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

@CName("kn_runFailureModuleSmokeTest")
fun kn_runFailureModuleSmokeTest(
    dbDir: CPointer<ByteVar>?,
    bundleName: CPointer<ByteVar>?,
    moduleName: CPointer<ByteVar>?,
): CPointer<ByteVar>? {
    val dir = dbDir?.toKString().orEmpty()
    val bundle = bundleName?.toKString().orEmpty()
    val module = moduleName?.toKString().orEmpty()
    val report = try {
        buildFailureModuleSmokeReport(dir, bundle, module)
    } catch (t: Throwable) {
        "Kotlin 异常: ${t::class.simpleName}: ${t.message}\n${t.stackTraceToString()}"
    }
    return strdup(report)
}
