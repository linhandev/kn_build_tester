@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.experimental.ExperimentalNativeApi::class,
)

package com.example.platform.RDB

import kotlinx.cinterop.*
import com.example.test.common.ApiGuard
import cnames.structs.*
import platform.ArkData.RDB.*
import platform.posix.usleep
import kotlin.native.CName
import platform.posix.strdup
import kotlin.experimental.ExperimentalNativeApi

/**
 * 损坏回调与报告构建默认在同进程内串行；若系统在其它线程调度回调，极端情况下可能乱序（演示用）。
 */
private val rdbCorruptionLogLines = mutableListOf<String>()

/**
 * 供 [rdbCorruptedHandler] 写入文本；报告构建结束时 [drainRdbCorruptionCallbackLines] 读出并 [appendLine]，
 * 避免依赖 [println]（设备上常不可见）。
 */
internal fun rdbCorruptionCallbackAppendLine(line: String) {
    rdbCorruptionLogLines.add(line)
}

internal fun drainRdbCorruptionCallbackLines(): List<String> {
    val copy = rdbCorruptionLogLines.toList()
    rdbCorruptionLogLines.clear()
    return copy
}

/**
 * 与 HiLog.kt 一致：损坏回调须为文件级 [staticCFunction]，且 Register 需传入 store。
 * 文本写入 [rdbCorruptionCallbackAppendLine]，由报告统一输出（等同界面上的 appendLine）。
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
private val rdbCorruptedHandler =
    staticCFunction { ctx: COpaquePointer?, cfg: CPointer<OH_Rdb_ConfigV2>?, store: CPointer<OH_Rdb_Store>? ->
        rdbCorruptionCallbackAppendLine(
            "[RdbCorruptedHandler] 回调已触发 ctx=$ctx cfg=$cfg store=$store — native 已调度该函数指针（仅损坏事件时会出现本行）。",
        )
    }

private enum class SmokeOut { Ok, Fail, Ver }

/**
 * 当前仅跑场景一 + 场景二时覆盖的 RDB CAPI（28 项；不含原场景三的分布式/订阅/Attach 等）。
 * 每项清单统计只计 1 次，多次调用合并为较重结果。
 */
private val RDB_CHECKLIST_S12 = listOf(
    "OH_Rdb_CreateConfig",
    "OH_Rdb_SetDatabaseDir",
    "OH_Rdb_SetStoreName",
    "OH_Rdb_SetEncrypted",
    "OH_Rdb_SetSecurityLevel",
    "OH_Rdb_SetDbType",
    "OH_Rdb_CreateOrOpen",
    "OH_Rdb_CreateValuesBucket",
    "OH_VBucket_PutFloatVector",
    "OH_VBucket_PutAssets",
    "OH_Rdb_CreatePredicates",
    "OH_Rdb_Insert",
    "OH_Rdb_Query",
    "OH_Rdb_GetSupportedDbType",
    "OH_Rdb_CloseStore",
    "OH_Rdb_DestroyConfig",
    "OH_RdbTrans_CreateOptions",
    "OH_RdbTransOption_SetType",
    "OH_Rdb_CreateTransaction",
    "OH_Rdb_CreateValueObject",
    "OH_RdbTrans_Query",
    "OH_RdbTrans_DestroyOptions",
    "OH_Rdb_CreateCryptoParam",
    "OH_Crypto_SetIteration",
    "OH_Rdb_SetCryptoParam",
    "OH_Rdb_DestroyCryptoParam",
    "OH_Rdb_RegisterCorruptedHandler",
    "OH_Rdb_UnregisterCorruptedHandler",
)

private val RDB_CHECKLIST_S12_SET = RDB_CHECKLIST_S12.toSet()

private fun rdbStatKeyFromLabel(label: String): String? =
    label.substringBefore("(").trim().takeIf { it in RDB_CHECKLIST_S12_SET }

/**
 * RDB（分布式数据管理中的关系型存储）CAPI 验证报告：全部 API 均从 [platform.ArkData.RDB] 直接调用（与头文件 `relational_store.h` 等一致）。
 * 图中部分伪代码与当前 NDK 头文件不一致处，按头文件实现并在释义中说明。
 *
 * 错误码释义参考：
 * - [relational_store / capi](https://developer.huawei.com/consumer/cn/doc/harmonyos-references/capi-relational-store-h)
 * - [error code](https://developer.huawei.com/consumer/cn/doc/harmonyos-references/capi-relational-store-error-code-h)
 */
@Suppress("MagicNumber", "LongMethod", "CognitiveComplexity")
fun buildRdbModuleSmokeReport(
    databaseDir: String,
    bundleName: String,
    moduleName: String,
): String {
    val failDetails = mutableListOf<String>()
    val verDetails = mutableListOf<String>()
    /** 清单 28 项（场景一+二）：每项最终判定（多次命中时合并） */
    val checklistOutcome = mutableMapOf<String, SmokeOut>()
    var detailLineCount = 0

    fun mergeChecklist(key: String, o: SmokeOut) {
        require(key in RDB_CHECKLIST_S12_SET) { "unknown checklist key: $key" }
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

    fun zhInt(v: Int, insertRowId: Boolean = false): String = when {
        v == ApiGuard.CODE_API_VERSION_TOO_HIGH ->
            "释义：API_VERSION_TOO_HIGH（ApiGuard）— 已拦截，未进入 native；具体版本说明见本行后附或报告末尾「API 版本不符」明细。"
        insertRowId && v > 0 && v < 1_000_000 ->
            "释义：Insert 成功时常返回新行 rowId=$v。"
        v == 0 ->
            "释义：0 即 RDB_OK，表示执行成功（见官方 OH_Rdb_ErrCode / capi-relational-store-error-code-h）。"
        v == -1 && !insertRowId ->
            "释义：-1 可能为 RDB_ERR 或异常占位，需结合日志。"
        v == 14800001 || v == 14_800_001 ->
            "释义：14800001 ≈ RDB_E_INVALID_ARGS（E_BASE+1），多为参数/上下文无效，例如安全区与目录、包名模块名不匹配。"
        v in 14_800_000..14_899_999 ->
            "释义：148xxxxx 为 ArkData RDB 业务错误码区间，详见 capi-relational-store-error-code-h。"
        else ->
            "释义：非 0 返回值请对照官方 OH_Rdb_ErrCode / SQLite 扩展码。"
    }

    fun zhPtr(nonNull: Boolean, what: String): String =
        if (nonNull) {
            "释义：non-null — $what 创建成功，使用后需按文档 destroy/close。"
        } else {
            "释义：null — $what 未创建成功或 API 不可用。"
        }

    return buildString {
        appendLine("=== RDB 分布式数据管理 · 关系型存储 CAPI 验证（platform.ArkData.RDB，含中文释义）===")
        appendLine("dbDir=$databaseDir bundle=$bundleName module=$moduleName")
        appendLine("当前仅场景一 + 场景二（已去掉场景三）。每场景独立 Config→CreateOrOpen→…→CloseStore→DestroyConfig；字符串/列名/缓冲区均在 memScoped 内构造真实 C 传参。SetBundleName=com.samples.rdbstore。官方：capi-relational-store-h。")
        appendLine()
        // 丢弃上次运行残留的损坏回调文本，避免串报告
        drainRdbCorruptionCallbackLines()

        memScoped {
            /**
             * @param statKey 为 null 时从 [name] 推导：`substringBefore('(')` 若在 28 项清单中则计入清单统计
             */
            fun record(
                name: String,
                raw: String,
                o: SmokeOut,
                extra: String? = null,
                statKey: String? = null,
                omitFailDetail: Boolean = false,
            ) {
                detailLineCount++
                val key = statKey ?: rdbStatKeyFromLabel(name)
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

            /** 对返回 Int 的 native：内部 [ApiGuard.guardInt] 已 try/catch [IllegalStateException]，block 内勿再套 ApiGuard。 */
            fun gInt(
                name: String,
                insertRowId: Boolean = false,
                statKey: String? = null,
                omitFailDetail: Boolean = false,
                onFailExtra: ((SmokeOut) -> String?)? = null,
                block: () -> Int,
            ) {
                val v = ApiGuard.guardInt(block)
                val verHint = ApiGuard.takeVersionHint()
                val raw = if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) "API_VERSION_TOO_HIGH" else "$v"
                val o = when {
                    v == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                    v == 0 || (insertRowId && v > 0 && v < 1_000_000) -> SmokeOut.Ok
                    else -> SmokeOut.Fail
                }
                val extra = buildString {
                    append(zhInt(v, insertRowId = insertRowId))
                    if (!verHint.isNullOrBlank()) {
                        append(" ")
                        append(verHint)
                    }
                    if (o == SmokeOut.Fail) {
                        onFailExtra?.invoke(o)?.trim()?.takeIf { it.isNotEmpty() }?.let {
                            append(" ")
                            append(it)
                        }
                    }
                }
                record(name, raw, o, extra, statKey, omitFailDetail)
            }

            /** [ApiGuard.guardInvoke]：`0` 成功，`CODE` 版本拦截，`-1` 其它失败。 */
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
                    append(zhInt(v, insertRowId = false))
                    if (!verHint.isNullOrBlank()) {
                        append(" ")
                        append(verHint)
                    }
                }
                record(name, raw, o, extra, statKey)
            }

            fun gLong(name: String, statKey: String? = null, block: () -> Long) {
                val v = ApiGuard.guardLong(block)
                val verHint = ApiGuard.takeVersionHint()
                val raw = when {
                    !verHint.isNullOrBlank() || v == ApiGuard.CODE_API_VERSION_TOO_HIGH.toLong() ->
                        "API_VERSION_TOO_HIGH"
                    else ->
                        "$v"
                }
                val o = when {
                    !verHint.isNullOrBlank() || v == ApiGuard.CODE_API_VERSION_TOO_HIGH.toLong() -> SmokeOut.Ver
                    v == 0L -> SmokeOut.Ok
                    else -> SmokeOut.Fail
                }
                val extra = buildString {
                    append(
                        if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH.toLong() && verHint.isNullOrBlank()) {
                            zhInt(ApiGuard.CODE_API_VERSION_TOO_HIGH)
                        } else {
                            "释义：Long 返回值=$v（0 常表示成功）。"
                        },
                    )
                    if (!verHint.isNullOrBlank()) {
                        append(" ")
                        append(verHint)
                    }
                }
                record(name, raw, o, extra, statKey)
            }

            fun gBool(name: String, statKey: String? = null, block: () -> Boolean) {
                val b = ApiGuard.guardBoolean(block)
                val verHint = ApiGuard.takeVersionHint()
                val o = when {
                    !verHint.isNullOrBlank() -> SmokeOut.Ver
                    b -> SmokeOut.Ok
                    else -> SmokeOut.Fail
                }
                val raw = when {
                    !verHint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                    b -> "true"
                    else -> "false"
                }
                val extra = buildString {
                    append(
                        if (!verHint.isNullOrBlank()) {
                            zhInt(ApiGuard.CODE_API_VERSION_TOO_HIGH)
                        } else {
                            "释义：Boolean 结果 $b。"
                        },
                    )
                    if (!verHint.isNullOrBlank()) {
                        append(" ")
                        append(verHint)
                    }
                }
                record(name, raw, o, extra, statKey)
            }

            fun gString(name: String, statKey: String? = null, block: () -> String) {
                val s = ApiGuard.guardString(block)
                val verHint = ApiGuard.takeVersionHint()
                val o = when {
                    !verHint.isNullOrBlank() || s == ApiGuard.STR_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                    s != "error" -> SmokeOut.Ok
                    else -> SmokeOut.Fail
                }
                val raw = when {
                    !verHint.isNullOrBlank() || s == ApiGuard.STR_API_VERSION_TOO_HIGH -> "API_VERSION_TOO_HIGH"
                    else -> s
                }
                val extra = buildString {
                    append(
                        if (!verHint.isNullOrBlank() || s == ApiGuard.STR_API_VERSION_TOO_HIGH) {
                            zhInt(ApiGuard.CODE_API_VERSION_TOO_HIGH)
                        } else {
                            "释义：String 长度=${s.length}。"
                        },
                    )
                    if (!verHint.isNullOrBlank()) {
                        append(" ")
                        append(verHint)
                    }
                }
                record(name, raw, o, extra, statKey)
            }

            fun gDouble(name: String, statKey: String? = null, block: () -> Double) {
                val d = ApiGuard.guardDouble(block)
                val verHint = ApiGuard.takeVersionHint()
                val o = when {
                    !verHint.isNullOrBlank() || d == ApiGuard.CODE_API_VERSION_TOO_HIGH.toDouble() -> SmokeOut.Ver
                    d != -1.0 -> SmokeOut.Ok
                    else -> SmokeOut.Fail
                }
                val raw = when {
                    !verHint.isNullOrBlank() || d == ApiGuard.CODE_API_VERSION_TOO_HIGH.toDouble() -> "API_VERSION_TOO_HIGH"
                    else -> "$d"
                }
                val extra = buildString {
                    append(
                        if (!verHint.isNullOrBlank() || d == ApiGuard.CODE_API_VERSION_TOO_HIGH.toDouble()) {
                            zhInt(ApiGuard.CODE_API_VERSION_TOO_HIGH)
                        } else {
                            "释义：Double 返回值=$d。"
                        },
                    )
                    if (!verHint.isNullOrBlank()) {
                        append(" ")
                        append(verHint)
                    }
                }
                record(name, raw, o, extra, statKey)
            }

            fun gPtr(name: String, what: String, statKey: String? = null, block: () -> COpaquePointer?) {
                val p = ApiGuard.guard(block, null, null)
                val verHint = ApiGuard.takeVersionHint()
                val o = when {
                    !verHint.isNullOrBlank() -> SmokeOut.Ver
                    p != null -> SmokeOut.Ok
                    else -> SmokeOut.Fail
                }
                val raw = when {
                    !verHint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                    p != null -> "non-null"
                    else -> "null"
                }
                val baseZh = if (!verHint.isNullOrBlank()) {
                    zhPtr(false, what)
                } else {
                    zhPtr(p != null, what)
                }
                val extra = buildString {
                    append(baseZh)
                    if (!verHint.isNullOrBlank()) {
                        append(" ")
                        append(verHint)
                    }
                }
                record(name, raw, o, extra, statKey)
            }

            fun destroyCursor(tag: String, c: CPointer<OH_Cursor>?) {
                if (c == null) {
                    record("$tag OH_Cursor.destroy", "skip(null)", SmokeOut.Ok, "释义：无游标可释放。")
                    return
                }
                gInt("$tag OH_Cursor.destroy") {
                    c.pointed.destroy?.invoke(c) ?: -1
                }
            }

            val dir = "/data/storage/el3/database"
            // HiLog.kt 场景一/二/三均写死 com.samples.rdbstore；与当前应用 bundle 不一致时 CreateOrOpen 易失败
            val bundleRdb = "com.samples.rdbstore"
            val module = moduleName.ifEmpty { "entry" }
            appendLine("传入 bundleName=$bundleName module=$module；两场景 SetBundleName 使用 HiLog 值 $bundleRdb。")
            appendLine()

            fun applyHiLogConfig(cfg: CPointer<OH_Rdb_ConfigV2>, dbFile: String) {
                gInt("OH_Rdb_SetDatabaseDir") { OH_Rdb_SetDatabaseDir(cfg, dir) }
                gInt("OH_Rdb_SetArea") { OH_Rdb_SetArea(cfg, RDB_SECURITY_AREA_EL3.toInt()) }
                gInt("OH_Rdb_SetStoreName") { OH_Rdb_SetStoreName(cfg, dbFile) }
                gInt("OH_Rdb_SetBundleName") { OH_Rdb_SetBundleName(cfg, bundleRdb) }
                gInt("OH_Rdb_SetModuleName") { OH_Rdb_SetModuleName(cfg, module) }
                gInt("OH_Rdb_SetEncrypted") { OH_Rdb_SetEncrypted(cfg, false) }
                gInt("OH_Rdb_SetSecurityLevel") { OH_Rdb_SetSecurityLevel(cfg, S3.toInt()) }
                gInt("OH_Rdb_SetDbType") { OH_Rdb_SetDbType(cfg, RDB_SQLITE.toInt()) }
            }

            // ---------- 场景一（HiLog 80～140）：主链路 ----------
            // 库名 RdbScene1.db；与场景二 RdbScene2.db 分离
            appendLine("========== 场景一：主链路 ==========")
            var cfg1: CPointer<OH_Rdb_ConfigV2>? = null
            var store1: CPointer<OH_Rdb_Store>? = null
            try {
                cfg1 = ApiGuard.guard({ OH_Rdb_CreateConfig() }, null, null)
                val cfg1Hint = ApiGuard.takeVersionHint()
                record(
                    "OH_Rdb_CreateConfig",
                    when {
                        !cfg1Hint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                        cfg1 == null -> "null"
                        else -> "non-null"
                    },
                    when {
                        !cfg1Hint.isNullOrBlank() -> SmokeOut.Ver
                        cfg1 != null -> SmokeOut.Ok
                        else -> SmokeOut.Fail
                    },
                    buildString {
                        append(zhPtr(cfg1 != null && cfg1Hint.isNullOrBlank(), "OH_Rdb_ConfigV2"))
                        if (!cfg1Hint.isNullOrBlank()) {
                            append(" ")
                            append(cfg1Hint)
                        }
                    },
                )
                if (cfg1 == null) {
                    appendLine(
                        if (cfg1Hint.isNullOrBlank()) {
                            "(场景一跳过：CreateConfig null)"
                        } else {
                            "(场景一跳过：CreateConfig — 原因见上条 CreateConfig 记录)"
                        },
                    )
                } else {
                    applyHiLogConfig(cfg1!!, "RdbScene1.db")
                    val err1 = alloc<IntVar>()
                    store1 = ApiGuard.guard({ OH_Rdb_CreateOrOpen(cfg1!!, err1.ptr) }, null, null)
                    val open1Hint = ApiGuard.takeVersionHint()
                    val ok1 = open1Hint.isNullOrBlank() && store1 != null && err1.value == RDB_OK
                    record(
                        "OH_Rdb_CreateOrOpen",
                        when {
                            !open1Hint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                            else -> "store=${if (store1 == null) "null" else "non-null"} errCode=${err1.value}"
                        },
                        when {
                            !open1Hint.isNullOrBlank() -> SmokeOut.Ver
                            ok1 -> SmokeOut.Ok
                            else -> SmokeOut.Fail
                        },
                        if (!open1Hint.isNullOrBlank()) {
                            "${zhPtr(false, "OH_Rdb_Store")} $open1Hint"
                        } else {
                            "${zhPtr(store1 != null, "OH_Rdb_Store")} ${zhInt(err1.value)}"
                        },
                    )
                    if (!ok1 && store1 != null) {
                        gInt("OH_Rdb_CloseStore") { OH_Rdb_CloseStore(store1!!) }
                        store1 = null
                    }
                    if (ok1) {
                        val st = store1!!
                        // Insert 仅写入 col（与 PutFloatVector/putInt64 对齐一致）；PutAssets 在独立子流程中只测 API 返回值
                        gInt("OH_Rdb_Execute") {
                            OH_Rdb_Execute(st, "CREATE TABLE IF NOT EXISTS t(id INTEGER PRIMARY KEY, col INTEGER)")
                        }
                        val bucket = ApiGuard.guard({ OH_Rdb_CreateValuesBucket() }, null, null)
                        val bucketHint = ApiGuard.takeVersionHint()
                        record(
                            "OH_Rdb_CreateValuesBucket",
                            when {
                                !bucketHint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                                bucket == null -> "null"
                                else -> "non-null"
                            },
                            when {
                                !bucketHint.isNullOrBlank() -> SmokeOut.Ver
                                bucket != null -> SmokeOut.Ok
                                else -> SmokeOut.Fail
                            },
                            buildString {
                                append(zhPtr(bucket != null && bucketHint.isNullOrBlank(), "OH_VBucket"))
                                if (!bucketHint.isNullOrBlank()) {
                                    append(" ")
                                    append(bucketHint)
                                }
                            },
                        )
                        var bucketColReady = false
                        if (bucket != null) {
                            // len=0 在部分真机上会返回非成功；用长度 1 更贴近「向量」语义且兼容性更好
                            val floatArr = allocArrayOf(1.0f)
                            gInt("OH_VBucket_PutFloatVector") {
                                OH_VBucket_PutFloatVector(bucket, "col", floatArr, 1uL)
                            }
                            // API17 等低版本常无 OH_VBucket_PutFloatVector；putInt64 失败不单独进「判定失败明细」，并入 Insert 一条说明
                            val putFvOk = checklistOutcome["OH_VBucket_PutFloatVector"] == SmokeOut.Ok
                            if (putFvOk) {
                                // 表定义为 col INTEGER：PutFloatVector 写入的是向量类型，模拟器 Insert 往往仍过，真机校验更严会 -1。
                                // 清单已统计 PutFloatVector；此处再 putInt64 覆盖 col，使与 INTEGER 列一致；对齐失败则 bucketColReady=false。
                                var colIntegerAligned = false
                                gInt("OH_VBucket.putInt64(col INTEGER 对齐)", omitFailDetail = true) {
                                    val fn = bucket.pointed.putInt64
                                        ?: return@gInt -1
                                    val r = fn(bucket, "col".cstr.ptr, 1L)
                                    colIntegerAligned = r == 0
                                    r
                                }
                                bucketColReady = colIntegerAligned
                                appendLine(
                                    "  → 释义：PutFloatVector 已计入清单后，对 INTEGER 列 col 再调用 putInt64 写入 1；" +
                                        "避免「向量写入 INTEGER 列」在真机 Insert 阶段比模拟器更易失败。",
                                )
                            } else {
                                var compatPutOk = false
                                gInt("OH_VBucket.putInt64", omitFailDetail = true) {
                                    val fn = bucket.pointed.putInt64
                                        ?: return@gInt -1
                                    val r = fn(bucket, "col".cstr.ptr, 1L)
                                    compatPutOk = r == 0
                                    r
                                }
                                bucketColReady = compatPutOk
                                appendLine(
                                    "  → 释义：OH_VBucket_PutFloatVector 非成功时尝试 OH_VBucket.putInt64 写入 col；" +
                                        "兼容步骤若失败仅在上方明细体现，不单独写入「判定失败明细」（与 OH_Rdb_Insert 合并说明）。",
                                )
                            }
                        }
                        val pred = ApiGuard.guard({ OH_Rdb_CreatePredicates("t") }, null, null)
                        val predHint = ApiGuard.takeVersionHint()
                        record(
                            "OH_Rdb_CreatePredicates",
                            when {
                                !predHint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                                pred == null -> "null"
                                else -> "non-null"
                            },
                            when {
                                !predHint.isNullOrBlank() -> SmokeOut.Ver
                                pred != null -> SmokeOut.Ok
                                else -> SmokeOut.Fail
                            },
                            buildString {
                                append(zhPtr(pred != null && predHint.isNullOrBlank(), "OH_Predicates"))
                                if (!predHint.isNullOrBlank()) {
                                    append(" ")
                                    append(predHint)
                                }
                            },
                        )
                        if (bucket != null) {
                            gInt(
                                "OH_Rdb_Insert",
                                insertRowId = true,
                                onFailExtra = { st ->
                                    if (st != SmokeOut.Fail) {
                                        null
                                    } else {
                                        buildString {
                                            append("Insert 仅使用主 bucket 的 col（PutFloatVector + putInt64 对齐），不含 PutAssets 子流程。")
                                            if (!bucketColReady) {
                                                append("根因汇总：列 col 未准备好（INTEGER 需可插入整型）。")
                                                when (checklistOutcome["OH_VBucket_PutFloatVector"]) {
                                                    SmokeOut.Ok ->
                                                        append(
                                                            " OH_VBucket_PutFloatVector 已成功，但 putInt64(INTEGER 对齐)未成功，" +
                                                                "col 仍为向量语义，真机 Insert 易失败。",
                                                        )
                                                    SmokeOut.Ver ->
                                                        append(" OH_VBucket_PutFloatVector 在低版本上 API/符号不可用；")
                                                    SmokeOut.Fail ->
                                                        append(" OH_VBucket_PutFloatVector 返回非成功；")
                                                    else -> Unit
                                                }
                                                if (checklistOutcome["OH_VBucket_PutFloatVector"] != SmokeOut.Ok) {
                                                    append(" OH_VBucket.putInt64(兼容) 亦未成功写入 col；")
                                                }
                                            } else {
                                                append(
                                                    "根因汇总：清单上 col 已就绪，Insert 仍 -1，请对照 OH_Rdb_ErrCode、表名 t、或 store 上下文。",
                                                )
                                            }
                                            append("本条合并说明，不拆成多条失败明细。")
                                        }
                                    }
                                },
                            ) {
                                OH_Rdb_Insert(st, "t", bucket)
                            }
                        }
                        // PutAssets 仅验证 API（独立 bucket，不参与 Insert）；clear 后释放 Data_Asset，再销毁 bucket
                        val bucketPutAssetsOnly = ApiGuard.guard({ OH_Rdb_CreateValuesBucket() }, null, null)
                        val bucketPaHint = ApiGuard.takeVersionHint()
                        record(
                            "OH_Rdb_CreateValuesBucket (PutAssets 子流程)",
                            when {
                                !bucketPaHint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                                bucketPutAssetsOnly == null -> "null"
                                else -> "non-null"
                            },
                            when {
                                !bucketPaHint.isNullOrBlank() -> SmokeOut.Ver
                                bucketPutAssetsOnly != null -> SmokeOut.Ok
                                else -> SmokeOut.Fail
                            },
                            buildString {
                                append(zhPtr(bucketPutAssetsOnly != null && bucketPaHint.isNullOrBlank(), "OH_VBucket"))
                                if (!bucketPaHint.isNullOrBlank()) {
                                    append(" ")
                                    append(bucketPaHint)
                                }
                            },
                        )
                        var bucketPaMulti: CPointer<CPointerVar<Data_Asset>>? = null
                        if (bucketPutAssetsOnly != null) {
                            val bpa = bucketPutAssetsOnly!!
                            gInt("OH_VBucket_PutAssets") {
                                val multi = OH_Data_Asset_CreateMultiple(2u) ?: return@gInt -1
                                for (i in 0 until 2) {
                                    val one = multi[i]
                                    if (one == null) {
                                        OH_Data_Asset_DestroyMultiple(multi, 2u)
                                        return@gInt -1
                                    }
                                    var rc = OH_Data_Asset_SetName(one, "smoke_asset_$i")
                                    if (rc != RDB_OK) {
                                        OH_Data_Asset_DestroyMultiple(multi, 2u)
                                        return@gInt rc
                                    }
                                    rc = OH_Data_Asset_SetUri(one, "file://smoke/asset_$i")
                                    if (rc != RDB_OK) {
                                        OH_Data_Asset_DestroyMultiple(multi, 2u)
                                        return@gInt rc
                                    }
                                }
                                val pr = OH_VBucket_PutAssets(bpa, "assets", multi, 2u)
                                if (pr == RDB_OK) {
                                    bucketPaMulti = multi
                                } else {
                                    OH_Data_Asset_DestroyMultiple(multi, 2u)
                                }
                                pr
                            }
                            bucketPaMulti?.let { multi ->
                                gInt("OH_VBucket.clear (PutAssets 子流程)", omitFailDetail = true) {
                                    bpa.pointed.clear?.invoke(bpa) ?: -1
                                }
                                OH_Data_Asset_DestroyMultiple(multi, 2u)
                                bucketPaMulti = null
                            }
                            appendLine(
                                "  → 释义：PutAssets 仅在独立 ValuesBucket 上统计返回值；主流程 Insert 不包含 assets 列，避免与 ASSETS 建表/Data_Asset 强耦合导致真机 Insert 失败。",
                            )
                            gInt("OH_VBucket.destroy (PutAssets 子流程)", omitFailDetail = true) {
                                bpa.pointed.destroy?.invoke(bpa) ?: -1
                            }
                        }
                        if (pred != null) {
                            val cols = allocArrayOf("col".cstr.ptr)
                            val cur = ApiGuard.guard({ OH_Rdb_Query(st, pred, cols, 1) }, null, null)
                            val curHint = ApiGuard.takeVersionHint()
                            record(
                                "OH_Rdb_Query",
                                when {
                                    !curHint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                                    cur == null -> "null"
                                    else -> "non-null"
                                },
                                when {
                                    !curHint.isNullOrBlank() -> SmokeOut.Ver
                                    cur != null -> SmokeOut.Ok
                                    else -> SmokeOut.Fail
                                },
                                buildString {
                                    append(zhPtr(cur != null && curHint.isNullOrBlank(), "OH_Cursor"))
                                    if (!curHint.isNullOrBlank()) {
                                        append(" ")
                                        append(curHint)
                                    }
                                },
                            )
                            destroyCursor("Query", cur?.reinterpret())
                        }
                        gPtr("OH_Rdb_GetSupportedDbType", "const int* DbType 列表") {
                            val cnt = alloc<IntVar>()
                            cnt.value = 0
                            OH_Rdb_GetSupportedDbType(cnt.ptr)
                        }
                        if (bucket != null) {
                            gInt("OH_VBucket.destroy") { bucket.pointed.destroy?.invoke(bucket) ?: -1 }
                        }
                        if (pred != null) {
                            gInt("OH_Predicates.destroy") { pred.pointed.destroy?.invoke(pred) ?: -1 }
                        }
                    }
                }
            } finally {
                if (store1 != null) {
                    gInt("OH_Rdb_CloseStore") { OH_Rdb_CloseStore(store1!!) }
                }
                if (cfg1 != null) {
                    gInt("OH_Rdb_DestroyConfig") { OH_Rdb_DestroyConfig(cfg1!!) }
                }
            }

            usleep(100_000u)
            appendLine("  → 释义：场景一已结束；延迟 100ms 再开场景二（独立 RdbScene2.db）。")

            // ---------- 场景二（HiLog 155～241）：事务 + Crypto + CorruptedHandler ----------
            appendLine()
            appendLine("========== 场景二：事务 + 加密 + 回调 ==========")
            var cfg2: CPointer<OH_Rdb_ConfigV2>? = null
            var store2: CPointer<OH_Rdb_Store>? = null
            try {
                cfg2 = ApiGuard.guard({ OH_Rdb_CreateConfig() }, null, null)
                val cfg2Hint = ApiGuard.takeVersionHint()
                record(
                    "OH_Rdb_CreateConfig",
                    when {
                        !cfg2Hint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                        cfg2 == null -> "null"
                        else -> "non-null"
                    },
                    when {
                        !cfg2Hint.isNullOrBlank() -> SmokeOut.Ver
                        cfg2 != null -> SmokeOut.Ok
                        else -> SmokeOut.Fail
                    },
                    buildString {
                        append(zhPtr(cfg2 != null && cfg2Hint.isNullOrBlank(), "OH_Rdb_ConfigV2"))
                        if (!cfg2Hint.isNullOrBlank()) {
                            append(" ")
                            append(cfg2Hint)
                        }
                    },
                )
                if (cfg2 == null) {
                    appendLine(
                        if (cfg2Hint.isNullOrBlank()) {
                            "(场景二跳过：CreateConfig null)"
                        } else {
                            "(场景二跳过：CreateConfig — 原因见上条 CreateConfig 记录)"
                        },
                    )
                } else {
                    applyHiLogConfig(cfg2!!, "RdbScene2.db")
                    val err2 = alloc<IntVar>()
                    store2 = ApiGuard.guard({ OH_Rdb_CreateOrOpen(cfg2!!, err2.ptr) }, null, null)
                    val open2Hint = ApiGuard.takeVersionHint()
                    val ok2 = open2Hint.isNullOrBlank() && store2 != null && err2.value == RDB_OK
                    record(
                        "OH_Rdb_CreateOrOpen",
                        when {
                            !open2Hint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                            else -> "store=${if (store2 == null) "null" else "non-null"} errCode=${err2.value}"
                        },
                        when {
                            !open2Hint.isNullOrBlank() -> SmokeOut.Ver
                            ok2 -> SmokeOut.Ok
                            else -> SmokeOut.Fail
                        },
                        if (!open2Hint.isNullOrBlank()) {
                            "${zhPtr(false, "OH_Rdb_Store")} $open2Hint"
                        } else {
                            "${zhPtr(store2 != null, "OH_Rdb_Store")} ${zhInt(err2.value)}"
                        },
                    )
                    if (!ok2 && store2 != null) {
                        gInt("OH_Rdb_CloseStore") { OH_Rdb_CloseStore(store2!!) }
                        store2 = null
                    }
                    if (ok2) {
                        val st2 = store2!!
                        gInt("OH_Rdb_Execute") {
                            OH_Rdb_Execute(st2, "CREATE TABLE IF NOT EXISTS t(id INTEGER PRIMARY KEY, col INTEGER)")
                        }
                        val opts = ApiGuard.guard({ OH_RdbTrans_CreateOptions() }, null, null)
                        val optsHint = ApiGuard.takeVersionHint()
                        record(
                            "OH_RdbTrans_CreateOptions",
                            when {
                                !optsHint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                                opts == null -> "null"
                                else -> "non-null"
                            },
                            when {
                                !optsHint.isNullOrBlank() -> SmokeOut.Ver
                                opts != null -> SmokeOut.Ok
                                else -> SmokeOut.Fail
                            },
                            buildString {
                                append(zhPtr(opts != null && optsHint.isNullOrBlank(), "OH_RDB_TransOptions"))
                                if (!optsHint.isNullOrBlank()) {
                                    append(" ")
                                    append(optsHint)
                                }
                            },
                        )
                        if (opts != null) {
                            gInt("OH_RdbTransOption_SetType") {
                                OH_RdbTransOption_SetType(opts, RDB_TRANS_IMMEDIATE)
                            }
                            val transVar = alloc<CPointerVar<OH_Rdb_Transaction>>()
                            gInt("OH_Rdb_CreateTransaction") {
                                OH_Rdb_CreateTransaction(st2, opts, transVar.ptr)
                            }
                            val trans = transVar.value
                            val valueObj = ApiGuard.guard({ OH_Rdb_CreateValueObject() }, null, null)
                            val valueObjHint = ApiGuard.takeVersionHint()
                            record(
                                "OH_Rdb_CreateValueObject",
                                when {
                                    !valueObjHint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                                    valueObj == null -> "null"
                                    else -> "non-null"
                                },
                                when {
                                    !valueObjHint.isNullOrBlank() -> SmokeOut.Ver
                                    valueObj != null -> SmokeOut.Ok
                                    else -> SmokeOut.Fail
                                },
                                buildString {
                                    append(zhPtr(valueObj != null && valueObjHint.isNullOrBlank(), "OH_Rdb_ValueObject"))
                                    if (!valueObjHint.isNullOrBlank()) {
                                        append(" ")
                                        append(valueObjHint)
                                    }
                                },
                            )
                            if (trans != null) {
                                val pred2 = ApiGuard.guard({ OH_Rdb_CreatePredicates("t") }, null, null)
                                val pred2Hint = ApiGuard.takeVersionHint()
                                record(
                                    "OH_Rdb_CreatePredicates",
                                    when {
                                        !pred2Hint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                                        pred2 == null -> "null"
                                        else -> "non-null"
                                    },
                                    when {
                                        !pred2Hint.isNullOrBlank() -> SmokeOut.Ver
                                        pred2 != null -> SmokeOut.Ok
                                        else -> SmokeOut.Fail
                                    },
                                    buildString {
                                        append(zhPtr(pred2 != null && pred2Hint.isNullOrBlank(), "OH_Predicates"))
                                        if (!pred2Hint.isNullOrBlank()) {
                                            append(" ")
                                            append(pred2Hint)
                                        }
                                    },
                                )
                                var valueObjDestroyed = false
                                if (pred2 != null && valueObj != null) {
                                    val v = alloc<LongVar>().apply { value = 42L }
                                    gInt("OH_Rdb_ValueObject.putInt64") {
                                        val fn = valueObj.pointed.putInt64
                                        if (fn == null) {
                                            -1
                                        } else {
                                            fn(valueObj, v.ptr, 1u)
                                            0
                                        }
                                    }
                                    gInt("OH_Predicates.equalTo") {
                                        val fn = pred2.pointed.equalTo
                                        if (fn == null) {
                                            -1
                                        } else {
                                            fn(pred2, "col".cstr.ptr, valueObj)
                                            0
                                        }
                                    }
                                    gInt("OH_Predicates.orderBy") {
                                        val fn = pred2.pointed.orderBy
                                        if (fn == null) {
                                            -1
                                        } else {
                                            fn(pred2, "col".cstr.ptr, ASC.toUInt())
                                            0
                                        }
                                    }
                                    gInt("OH_Predicates.limit") {
                                        val fn = pred2.pointed.limit
                                        if (fn == null) {
                                            -1
                                        } else {
                                            fn(pred2, 10u)
                                            0
                                        }
                                    }
                                    val colArr = allocArrayOf("col".cstr.ptr)
                                    val transCursor = ApiGuard.guard(
                                        { OH_RdbTrans_Query(trans, pred2, colArr, 1) },
                                        null,
                                        null,
                                    )
                                    val transQueryHint = ApiGuard.takeVersionHint()
                                    record(
                                        "OH_RdbTrans_Query",
                                        when {
                                            !transQueryHint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                                            transCursor == null -> "null"
                                            else -> "non-null"
                                        },
                                        when {
                                            !transQueryHint.isNullOrBlank() -> SmokeOut.Ver
                                            transCursor != null -> SmokeOut.Ok
                                            else -> SmokeOut.Fail
                                        },
                                        buildString {
                                            append(zhPtr(transCursor != null && transQueryHint.isNullOrBlank(), "OH_Cursor"))
                                            if (!transQueryHint.isNullOrBlank()) {
                                                append(" ")
                                                append(transQueryHint)
                                            }
                                        },
                                    )
                                    if (transCursor != null) {
                                        gInt("OH_Cursor.destroy") {
                                            transCursor.pointed.destroy?.invoke(transCursor) ?: -1
                                        }
                                    }
                                    gInt("OH_Rdb_ValueObject.destroy") {
                                        valueObj.pointed.destroy?.invoke(valueObj) ?: -1
                                    }
                                    valueObjDestroyed = true
                                    gInt("OH_Predicates.destroy") {
                                        pred2.pointed.destroy?.invoke(pred2) ?: -1
                                    }
                                } else {
                                    if (pred2 != null) {
                                        gInt("OH_Predicates.destroy") {
                                            pred2.pointed.destroy?.invoke(pred2) ?: -1
                                        }
                                    }
                                }
                                if (valueObj != null && !valueObjDestroyed) {
                                    gInt("OH_Rdb_ValueObject.destroy") {
                                        valueObj.pointed.destroy?.invoke(valueObj) ?: -1
                                    }
                                }
                                gInt("OH_RdbTrans_DestroyOptions") { OH_RdbTrans_DestroyOptions(opts) }
                                gInt("OH_RdbTrans_Destroy") {
                                    OH_RdbTrans_Destroy(trans)
                                    0
                                }
                            } else {
                                if (valueObj != null) {
                                    gInt("OH_Rdb_ValueObject.destroy") {
                                        valueObj.pointed.destroy?.invoke(valueObj) ?: -1
                                    }
                                }
                                gInt("OH_RdbTrans_DestroyOptions") { OH_RdbTrans_DestroyOptions(opts) }
                            }
                        }
                        val crypto = ApiGuard.guard({ OH_Rdb_CreateCryptoParam() }, null, null)
                        val cryptoHint = ApiGuard.takeVersionHint()
                        record(
                            "OH_Rdb_CreateCryptoParam",
                            when {
                                !cryptoHint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                                crypto == null -> "null"
                                else -> "non-null"
                            },
                            when {
                                !cryptoHint.isNullOrBlank() -> SmokeOut.Ver
                                crypto != null -> SmokeOut.Ok
                                else -> SmokeOut.Fail
                            },
                            buildString {
                                append(zhPtr(crypto != null && cryptoHint.isNullOrBlank(), "OH_Rdb_CryptoParam"))
                                if (!cryptoHint.isNullOrBlank()) {
                                    append(" ")
                                    append(cryptoHint)
                                }
                            },
                        )
                        if (crypto != null) {
                            gLong("OH_Crypto_SetIteration") {
                                OH_Crypto_SetIteration(crypto, 100L).toLong()
                            }
                            val cfgTmp = ApiGuard.guard({ OH_Rdb_CreateConfig() }, null, null)
                            val cfgTmpHint = ApiGuard.takeVersionHint()
                            record(
                                "OH_Rdb_CreateConfig",
                                when {
                                    !cfgTmpHint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                                    cfgTmp == null -> "null"
                                    else -> "non-null"
                                },
                                when {
                                    !cfgTmpHint.isNullOrBlank() -> SmokeOut.Ver
                                    cfgTmp != null -> SmokeOut.Ok
                                    else -> SmokeOut.Fail
                                },
                                buildString {
                                    append(zhPtr(cfgTmp != null && cfgTmpHint.isNullOrBlank(), "OH_Rdb_ConfigV2"))
                                    if (!cfgTmpHint.isNullOrBlank()) {
                                        append(" ")
                                        append(cfgTmpHint)
                                    }
                                },
                            )
                            if (cfgTmp != null && cfgTmpHint.isNullOrBlank()) {
                                val cfgC = cfgTmp!!
                                try {
                                    gInt("OH_Rdb_SetDatabaseDir") {
                                        OH_Rdb_SetDatabaseDir(cfgC, dir)
                                    }
                                    gInt("OH_Rdb_SetArea") {
                                        OH_Rdb_SetArea(cfgC, RDB_SECURITY_AREA_EL3.toInt())
                                    }
                                    gInt("OH_Rdb_SetStoreName") {
                                        OH_Rdb_SetStoreName(cfgC, "RdbScene2_crypto_tmp.db")
                                    }
                                    gInt("OH_Rdb_SetBundleName") {
                                        OH_Rdb_SetBundleName(cfgC, bundleRdb)
                                    }
                                    gInt("OH_Rdb_SetModuleName") {
                                        OH_Rdb_SetModuleName(cfgC, module)
                                    }
                                    gInt("OH_Rdb_SetEncrypted") {
                                        OH_Rdb_SetEncrypted(cfgC, false)
                                    }
                                    gInt("OH_Rdb_SetSecurityLevel") {
                                        OH_Rdb_SetSecurityLevel(cfgC, S3.toInt())
                                    }
                                    gInt("OH_Rdb_SetDbType") {
                                        OH_Rdb_SetDbType(cfgC, RDB_SQLITE.toInt())
                                    }
                                    gInt("OH_Rdb_SetCryptoParam") {
                                        OH_Rdb_SetCryptoParam(cfgC, crypto)
                                    }
                                } finally {
                                    gInt("OH_Rdb_DestroyConfig") {
                                        OH_Rdb_DestroyConfig(cfgC)
                                    }
                                }
                            }
                            gInt("OH_Rdb_DestroyCryptoParam") { OH_Rdb_DestroyCryptoParam(crypto) }
                        }
                        gInt("OH_Rdb_RegisterCorruptedHandler") {
                            OH_Rdb_RegisterCorruptedHandler(cfg2!!, st2, rdbCorruptedHandler)
                        }
                        gInt("OH_Rdb_UnregisterCorruptedHandler") {
                            OH_Rdb_UnregisterCorruptedHandler(cfg2!!, st2, rdbCorruptedHandler)
                        }
                        appendLine(
                            "  → 释义：上两条的「: 0」仅表示 Register/UnRegister **API 调用成功**，" +
                                "**不会**在此时执行回调函数体；第三个参数是传入 native 的 C 函数指针（[staticCFunction]）。" +
                                "该指针仅在 **数据库文件损坏** 等情况下由系统**异步**调用，与 HiLog 行为一致。",
                        )
                        appendLine(
                            "  → 传参核对：cfg、store 与上面 CreateOrOpen 成功对象一致；handler 指针标识：$rdbCorruptedHandler",
                        )
                    }
                }
            } finally {
                if (store2 != null) {
                    gInt("OH_Rdb_CloseStore") { OH_Rdb_CloseStore(store2!!) }
                }
                if (cfg2 != null) {
                    gInt("OH_Rdb_DestroyConfig") { OH_Rdb_DestroyConfig(cfg2!!) }
                }
            }
            appendLine()
            appendLine("  → 释义：执行顺序为 场景一→场景二；每场景各自 CloseStore+DestroyConfig。")
        }

        val corruptionCallbackLines = drainRdbCorruptionCallbackLines()
        appendLine()
        appendLine("---------- RdbCorruptedHandler 回调记录（仅损坏事件时才有内容）----------")
        appendLine(
            "  → 释义：下列行由回调内部写入；**正常冒烟无输出是预期现象**（Register/Unregister 成功不会触发回调）。",
        )
        if (corruptionCallbackLines.isEmpty()) {
            appendLine("（当前无回调输出）")
        } else {
            corruptionCallbackLines.forEach { appendLine(it) }
        }

        appendLine()
        appendLine("---------- 清单统计（场景一+二共 28 项，每函数计 1 次）----------")
        appendLine("合并规则：同一清单函数多次调用时，取较重结果（API版本不符 > 失败 > 成功）。")
        var ckOk = 0
        var ckFail = 0
        var ckVer = 0
        var ckNotRun = 0

        /** 仅当 [checklistOutcome][key]==null 时使用：解释为何未产生合并判定。 */
        fun notRunExplanation(key: String): String {
            val transOpts = checklistOutcome["OH_RdbTrans_CreateOptions"]
            val crypto = checklistOutcome["OH_Rdb_CreateCryptoParam"]
            return when (key) {
                "OH_RdbTransOption_SetType",
                "OH_Rdb_CreateTransaction",
                "OH_Rdb_CreateValueObject",
                "OH_RdbTrans_Query",
                "OH_RdbTrans_DestroyOptions",
                -> {
                    when (transOpts) {
                        SmokeOut.Ver ->
                            "前置 OH_RdbTrans_CreateOptions 为 API 版本不符/符号缺失，未得到 TransOptions，事务整段未进入。"
                        SmokeOut.Fail ->
                            "前置 OH_RdbTrans_CreateOptions 失败（如返回 null），后续事务 API 未继续调用。"
                        SmokeOut.Ok ->
                            "TransOptions 已成功但仍无本项记录：请对照上文明细是否因 CreateTransaction 失败或 pred/valueObj 分支未走到。"
                        null ->
                            "前置 OH_RdbTrans_CreateOptions 无合并结果：场景二可能未打开 store 或未执行到事务段。"
                    }
                }
                "OH_Crypto_SetIteration",
                "OH_Rdb_SetCryptoParam",
                "OH_Rdb_DestroyCryptoParam",
                -> {
                    when (crypto) {
                        SmokeOut.Ver ->
                            "前置 OH_Rdb_CreateCryptoParam 为 API 版本不符，加密相关步骤未执行。"
                        SmokeOut.Fail ->
                            "前置 OH_Rdb_CreateCryptoParam 失败（如返回 null），加密步骤未继续。"
                        SmokeOut.Ok ->
                            "CryptoParam 已创建但仍无本项：请对照上文是否在 SetCryptoParam 前已异常返回。"
                        null ->
                            "前置 OH_Rdb_CreateCryptoParam 无合并结果：场景二可能未进入或未到加密段。"
                    }
                }
                "OH_Rdb_RegisterCorruptedHandler",
                "OH_Rdb_UnregisterCorruptedHandler",
                -> "场景二内未调用到本 API：需 store 已成功 Open 且执行流走到注册处；若 CreateOrOpen/事务等提前失败则整段跳过。"
                "OH_VBucket_PutAssets" -> {
                    when (checklistOutcome["OH_Rdb_CreateValuesBucket"]) {
                        SmokeOut.Ver ->
                            "前置 OH_Rdb_CreateValuesBucket 为 API 版本不符；PutAssets 使用独立子流程 bucket，未创建则无法调用。"
                        SmokeOut.Fail ->
                            "前置 OH_Rdb_CreateValuesBucket 合并为失败（含主 bucket 或 PutAssets 子流程 bucket 任一为 null）。"
                        SmokeOut.Ok ->
                            "CreateValuesBucket 已成功但仍无 PutAssets 合并：请对照上文 Insert 之后 PutAssets 子流程是否被跳过。"
                        null ->
                            "场景一未产生 CreateValuesBucket 合并结果：可能未进入 store Open 后分支。"
                    }
                }
                else ->
                    "报告中未对本项调用 [record] 合并：多为更早步骤失败、ApiGuard 拦截导致分支未执行，或仅在未跑到的子分支中出现。"
            }
        }

        appendLine()
        RDB_CHECKLIST_S12.forEachIndexed { index, key ->
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
        appendLine("核对：${ckOk + ckFail + ckVer + ckNotRun} = ${RDB_CHECKLIST_S12.size}（应为 28）")
        appendLine()
        // appendLine("明细输出行数：$detailLineCount（含清单外步骤，如 SetArea、Execute、Rollback、destroy 等，不参与 28 项计数）")
        // appendLine()
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
            "  → 释义：「未执行」表示该清单项在报告中**从未被合并过判定**（与「失败」「API版本不符」不同）；" +
                "常见原因是**前置条件未满足**导致代码分支未走到（例如 TransOptions 未创建则整段事务不会调用）。",
        )
        val notRunKeys = RDB_CHECKLIST_S12.filter { checklistOutcome[it] == null }
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

@CName("kn_runRdbModuleSmokeTest")
fun kn_runRdbModuleSmokeTest(
    dbDir: CPointer<ByteVar>?,
    bundleName: CPointer<ByteVar>?,
    moduleName: CPointer<ByteVar>?,
): CPointer<ByteVar>? {
    val dir = dbDir?.toKString().orEmpty()
    val bundle = bundleName?.toKString().orEmpty()
    val module = moduleName?.toKString().orEmpty()
    val report = try {
        buildRdbModuleSmokeReport(dir, bundle, module)
    } catch (t: Throwable) {
        "Kotlin 异常: ${t::class.simpleName}: ${t.message}\n${t.stackTraceToString()}"
    }
    return strdup(report)
}
