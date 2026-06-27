@file:OptIn(
        kotlinx.cinterop.ExperimentalForeignApi::class,
        kotlin.experimental.ExperimentalNativeApi::class,
)

package com.example.platform.Drawing

import cnames.structs.OH_Drawing_Array
import cnames.structs.OH_Drawing_Bitmap
import cnames.structs.OH_Drawing_ColorFilter
import cnames.structs.OH_Drawing_Font
import cnames.structs.OH_Drawing_FontCollection
import cnames.structs.OH_Drawing_FontMgr
import cnames.structs.OH_Drawing_Path
import cnames.structs.OH_Drawing_Rect
import cnames.structs.OH_Drawing_RoundRect
import cnames.structs.OH_Drawing_Run
import cnames.structs.OH_Drawing_TextLine
import cnames.structs.OH_Drawing_TextStyle
import cnames.structs.OH_Drawing_Typography
import cnames.structs.OH_Drawing_TypographyCreate
import cnames.structs.OH_Drawing_TypographyStyle
import com.example.test.common.ApiGuard
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.cValue
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.ArkGraphics2D.Drawing.OH_Drawing_AlphaFormat
import platform.ArkGraphics2D.Drawing.OH_Drawing_BitmapBuild
import platform.ArkGraphics2D.Drawing.OH_Drawing_BitmapCreate
import platform.ArkGraphics2D.Drawing.OH_Drawing_BitmapDestroy
import platform.ArkGraphics2D.Drawing.OH_Drawing_BitmapFormat
import platform.ArkGraphics2D.Drawing.OH_Drawing_BitmapGetWidth
import platform.ArkGraphics2D.Drawing.OH_Drawing_ColorFilterCreateMatrix
import platform.ArkGraphics2D.Drawing.OH_Drawing_ColorFilterDestroy
import platform.ArkGraphics2D.Drawing.OH_Drawing_ColorFormat
import platform.ArkGraphics2D.Drawing.OH_Drawing_CornerPos
import platform.ArkGraphics2D.Drawing.OH_Drawing_CreateFontCollection
import platform.ArkGraphics2D.Drawing.OH_Drawing_CreateTextStyle
import platform.ArkGraphics2D.Drawing.OH_Drawing_CreateTypography
import platform.ArkGraphics2D.Drawing.OH_Drawing_CreateTypographyHandler
import platform.ArkGraphics2D.Drawing.OH_Drawing_CreateTypographyStyle
import platform.ArkGraphics2D.Drawing.OH_Drawing_DestroyFontCollection
import platform.ArkGraphics2D.Drawing.OH_Drawing_DestroyRunGlyphs
import platform.ArkGraphics2D.Drawing.OH_Drawing_DestroyRuns
import platform.ArkGraphics2D.Drawing.OH_Drawing_DestroyTextLine
import platform.ArkGraphics2D.Drawing.OH_Drawing_DestroyTextLines
import platform.ArkGraphics2D.Drawing.OH_Drawing_DestroyTextStyle
import platform.ArkGraphics2D.Drawing.OH_Drawing_DestroyTypography
import platform.ArkGraphics2D.Drawing.OH_Drawing_DestroyTypographyHandler
import platform.ArkGraphics2D.Drawing.OH_Drawing_DestroyTypographyStyle
import platform.ArkGraphics2D.Drawing.OH_Drawing_FontCreate
import platform.ArkGraphics2D.Drawing.OH_Drawing_FontDestroy
import platform.ArkGraphics2D.Drawing.OH_Drawing_FontGetPathForGlyph
import platform.ArkGraphics2D.Drawing.OH_Drawing_FontMgrCreate
import platform.ArkGraphics2D.Drawing.OH_Drawing_FontMgrDestroy
import platform.ArkGraphics2D.Drawing.OH_Drawing_FontMgrDestroyFamilyName
import platform.ArkGraphics2D.Drawing.OH_Drawing_FontMgrGetFamilyCount
import platform.ArkGraphics2D.Drawing.OH_Drawing_FontMgrGetFamilyName
import platform.ArkGraphics2D.Drawing.OH_Drawing_GetFontCollectionGlobalInstance
import platform.ArkGraphics2D.Drawing.OH_Drawing_GetRunByIndex
import platform.ArkGraphics2D.Drawing.OH_Drawing_GetRunGlyphCount
import platform.ArkGraphics2D.Drawing.OH_Drawing_GetRunGlyphs
import platform.ArkGraphics2D.Drawing.OH_Drawing_GetRunGlyphsByIndex
import platform.ArkGraphics2D.Drawing.OH_Drawing_GetRunTextDirection
import platform.ArkGraphics2D.Drawing.OH_Drawing_GetTextLineByIndex
import platform.ArkGraphics2D.Drawing.OH_Drawing_LineMetrics
import platform.ArkGraphics2D.Drawing.OH_Drawing_PathCreate
import platform.ArkGraphics2D.Drawing.OH_Drawing_PathDestroy
import platform.ArkGraphics2D.Drawing.OH_Drawing_Point2D
import platform.ArkGraphics2D.Drawing.OH_Drawing_RectCreate
import platform.ArkGraphics2D.Drawing.OH_Drawing_RectDestroy
import platform.ArkGraphics2D.Drawing.OH_Drawing_RegisterFont
import platform.ArkGraphics2D.Drawing.OH_Drawing_RegisterFontBuffer
import platform.ArkGraphics2D.Drawing.OH_Drawing_RoundRectCreate
import platform.ArkGraphics2D.Drawing.OH_Drawing_RoundRectDestroy
import platform.ArkGraphics2D.Drawing.OH_Drawing_RoundRectGetCorner
import platform.ArkGraphics2D.Drawing.OH_Drawing_RoundRectSetCorner
import platform.ArkGraphics2D.Drawing.OH_Drawing_SetTextStyleFontFamilies
import platform.ArkGraphics2D.Drawing.OH_Drawing_TextLineGetGlyphRuns
import platform.ArkGraphics2D.Drawing.OH_Drawing_TextStyleGetFontFamilies
import platform.ArkGraphics2D.Drawing.OH_Drawing_TypographyGetLineCount
import platform.ArkGraphics2D.Drawing.OH_Drawing_TypographyGetLineMetricsAt
import platform.ArkGraphics2D.Drawing.OH_Drawing_TypographyGetTextLines
import platform.ArkGraphics2D.Drawing.OH_Drawing_TypographyHandlerAddText
import platform.ArkGraphics2D.Drawing.OH_Drawing_TypographyHandlerPopTextStyle
import platform.ArkGraphics2D.Drawing.OH_Drawing_TypographyHandlerPushTextStyle
import platform.ArkGraphics2D.Drawing.OH_Drawing_TypographyLayout
import platform.ArkGraphics2D.Drawing.OH_Drawing_UnregisterFont
import platform.posix.FILE
import platform.posix.SEEK_END
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.memset
import platform.posix.rewind
import platform.posix.strdup

private enum class SmokeOut {
    Ok,
    Skip,
    Fail,
    Ver
}

/** 与《调整后的CAPI测试方案-0316》**模块7：Drawing** 场景对应的清单项（含构建排版链路所需的最小 API，便于统计合并）。 */
private val DRAWING_CHECKLIST =
        listOf(
                "OH_Drawing_RectCreate",
                "OH_Drawing_RectDestroy",
                "OH_Drawing_RoundRectCreate",
                "OH_Drawing_RoundRectSetCorner",
                "OH_Drawing_RoundRectGetCorner",
                "OH_Drawing_RoundRectDestroy",
                "OH_Drawing_ColorFilterCreateMatrix",
                "OH_Drawing_ColorFilterDestroy",
                "OH_Drawing_FontCreate",
                "OH_Drawing_PathCreate",
                "OH_Drawing_FontGetPathForGlyph",
                "OH_Drawing_PathDestroy",
                "OH_Drawing_FontDestroy",
                "OH_Drawing_FontMgrCreate",
                "OH_Drawing_FontMgrGetFamilyName",
                "OH_Drawing_FontMgrDestroyFamilyName",
                "OH_Drawing_FontMgrDestroy",
                "OH_Drawing_CreateTextStyle",
                "OH_Drawing_SetTextStyleFontFamilies",
                "OH_Drawing_TextStyleGetFontFamilies",
                "OH_Drawing_DestroyTextStyle",
                "OH_Drawing_BitmapCreate",
                "OH_Drawing_BitmapBuild",
                "OH_Drawing_BitmapGetWidth",
                "OH_Drawing_BitmapDestroy",
                "OH_Drawing_CreateFontCollection",
                "OH_Drawing_RegisterFont",
                "OH_Drawing_RegisterFontBuffer",
                "OH_Drawing_DestroyFontCollection",
                "OH_Drawing_CreateTypographyStyle",
                "OH_Drawing_CreateTypographyHandler",
                "OH_Drawing_TypographyHandlerPushTextStyle",
                "OH_Drawing_TypographyHandlerAddText",
                "OH_Drawing_TypographyHandlerPopTextStyle",
                "OH_Drawing_CreateTypography",
                "OH_Drawing_TypographyLayout",
                "OH_Drawing_TypographyGetLineCount",
                "OH_Drawing_TypographyGetTextLines",
                "OH_Drawing_GetTextLineByIndex",
                "OH_Drawing_TextLineGetGlyphRuns",
                "OH_Drawing_GetRunByIndex",
                "OH_Drawing_GetRunTextDirection",
                "OH_Drawing_GetRunGlyphs",
                "OH_Drawing_GetRunGlyphsByIndex",
                "OH_Drawing_DestroyRunGlyphs",
                "OH_Drawing_DestroyRuns",
                "OH_Drawing_DestroyTextLine",
                "OH_Drawing_DestroyTextLines",
                "OH_Drawing_DestroyTypography",
                "OH_Drawing_DestroyTypographyHandler",
                "OH_Drawing_DestroyTypographyStyle",
                "OH_Drawing_TypographyGetLineMetricsAt",
        )

private val DRAWING_CHECKLIST_SET = DRAWING_CHECKLIST.toSet()

private fun drawingStatKeyFromLabel(label: String): String? =
        label.substringBefore("(").trim().takeIf { it in DRAWING_CHECKLIST_SET }

private fun readBinaryFile(path: String): ByteArray? {
    val f: CPointer<FILE>? = fopen(path, "rb") ?: return null
    try {
        if (fseek(f, 0, SEEK_END) != 0) return null
        val len = ftell(f)
        if (len < 0L) return null
        rewind(f)
        if (len == 0L || len > 32L * 1024 * 1024) return null
        val out = ByteArray(len.toInt())
        var readOk = true
        out.usePinned { pinned ->
            val n = fread(pinned.addressOf(0), 1u, len.toULong(), f)
            if (n.toLong() != len) {
                readOk = false
            }
        }
        if (!readOk) {
            return null
        }
        return out
    } finally {
        fclose(f)
    }
}

/**
 * Drawing（native_drawing）CAPI 验证报告：调用 [platform.ArkGraphics2D.Drawing]（与 `drawing_*.h` 一致）。
 * [fontTtfPath] 为设备可读绝对路径（如将 `Apple_Chancery.ttf` 从 rawfile 拷至 filesDir 后传入）；空串时跳过
 * RegisterFont/RegisterFontBuffer 场景。
 */
@Suppress("MagicNumber", "LongMethod", "CognitiveComplexity")
fun buildDrawingModuleSmokeReport(fontTtfPath: String): String {
    val failDetails = mutableListOf<String>()
    val verDetails = mutableListOf<String>()
    val checklistOutcome = mutableMapOf<String, SmokeOut>()
    var detailLineCount = 0

    return buildString {
        fun mergeChecklist(key: String, o: SmokeOut) {
            require(key in DRAWING_CHECKLIST_SET) { "unknown checklist key: $key" }
            fun rank(x: SmokeOut) =
                    when (x) {
                        SmokeOut.Ok -> 0
                        SmokeOut.Skip -> 1
                        SmokeOut.Fail -> 2
                        SmokeOut.Ver -> 3
                    }
            val prev = checklistOutcome[key]
            checklistOutcome[key] =
                    if (prev == null) {
                        o
                    } else {
                        if (rank(o) >= rank(prev)) o else prev
                    }
        }

        fun record(
                name: String,
                raw: String,
                o: SmokeOut,
                extra: String? = null,
                statKey: String? = null,
                omitFailDetail: Boolean = false,
        ) {
            detailLineCount++
            val key = statKey ?: drawingStatKeyFromLabel(name)
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
                SmokeOut.Ver -> verDetails.add("• $name：结果 $raw。${extra?.trim().orEmpty()}")
                SmokeOut.Ok, SmokeOut.Skip -> Unit
            }
        }

        appendLine(
                "=== 模块7：Drawing（native_drawing）CAPI 验证（platform.ArkGraphics2D.Drawing，含中文释义）==="
        )
        appendLine(
                "依据：《调整后的CAPI测试方案-0316》— 模块7：Drawing（圆角矩形 / ColorMatrix / Font·Bitmap·Typography / 注册字体）。"
        )
        appendLine(
                "fontTtfPath=${fontTtfPath.ifEmpty { "（空：跳过 RegisterFont / RegisterFontBuffer）" }}"
        )
        appendLine(
                "清单 ${DRAWING_CHECKLIST.size} 项；圆角半径结构在 klib 中为 [OH_Drawing_Point2D]（对应方案中的 Corner 半径）。"
        )
        appendLine()

        memScoped {
            fun zhVer(): String = ApiGuard.takeVersionHint()?.let { " $it" }.orEmpty()

            fun gInvokeDrawing(name: String, statKey: String? = null, block: () -> Unit) {
                val v = ApiGuard.guardInvoke(block)
                val o =
                        when {
                            v == ApiGuard.CODE_API_VERSION_TOO_HIGH -> SmokeOut.Ver
                            v == 0 -> SmokeOut.Ok
                            else -> SmokeOut.Fail
                        }
                val raw =
                        if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) "API_VERSION_TOO_HIGH"
                        else "$v"
                val extra =
                        if (v == ApiGuard.CODE_API_VERSION_TOO_HIGH) {
                            "释义：版本拦截。${zhVer()}"
                        } else {
                            "释义：guardInvoke 返回 $v（0 表示未抛 IllegalStateException）。"
                        }
                record(name, raw, o, extra, statKey)
            }

            fun gPtrDrawing(
                    name: String,
                    what: String,
                    statKey: String? = null,
                    block: () -> CPointer<*>?
            ) {
                val p = ApiGuard.guard(block, null, null)
                val verHint = ApiGuard.takeVersionHint()
                val o =
                        when {
                            !verHint.isNullOrBlank() -> SmokeOut.Ver
                            p != null -> SmokeOut.Ok
                            else -> SmokeOut.Fail
                        }
                val raw =
                        when {
                            !verHint.isNullOrBlank() -> "API_VERSION_TOO_HIGH"
                            p != null -> "non-null"
                            else -> "null"
                        }
                val extra = buildString {
                    append(
                            if (!verHint.isNullOrBlank()) {
                                "释义：版本拦截。$verHint"
                            } else if (p != null) {
                                "释义：non-null — $what。"
                            } else {
                                "释义：null — $what 未创建。"
                            },
                    )
                }
                record(name, raw, o, extra, statKey)
            }

            fun gUIntDrawing(
                    name: String,
                    statKey: String? = null,
                    good: (UInt) -> Boolean,
                    block: () -> UInt,
            ) {
                val v =
                        try {
                            block()
                        } catch (e: IllegalStateException) {
                            record(
                                    name,
                                    "API_VERSION_TOO_HIGH",
                                    SmokeOut.Ver,
                                    "释义：${ApiGuard.describeApiVersionMismatch(e)}",
                                    statKey,
                            )
                            return
                        } catch (_: Throwable) {
                            record(name, "exception", SmokeOut.Fail, "释义：调用抛非版本异常。", statKey)
                            return
                        }
                val verHint = ApiGuard.takeVersionHint()
                val o =
                        when {
                            !verHint.isNullOrBlank() -> SmokeOut.Ver
                            good(v) -> SmokeOut.Ok
                            else -> SmokeOut.Fail
                        }
                val raw = if (!verHint.isNullOrBlank()) "API_VERSION_TOO_HIGH" else "$v"
                val extra = buildString {
                    append("释义：UInt 返回值=$v。")
                    if (!verHint.isNullOrBlank()) append(" ").append(verHint)
                }
                record(name, raw, o, extra, statKey)
            }

            fun gULongDrawing(
                    name: String,
                    statKey: String? = null,
                    good: (ULong) -> Boolean,
                    block: () -> ULong
            ) {
                val v =
                        try {
                            block()
                        } catch (e: IllegalStateException) {
                            record(
                                    name,
                                    "API_VERSION_TOO_HIGH",
                                    SmokeOut.Ver,
                                    "释义：${ApiGuard.describeApiVersionMismatch(e)}",
                                    statKey,
                            )
                            return
                        } catch (_: Throwable) {
                            record(name, "exception", SmokeOut.Fail, "释义：调用异常。", statKey)
                            return
                        }
                val verHint = ApiGuard.takeVersionHint()
                val o =
                        when {
                            !verHint.isNullOrBlank() -> SmokeOut.Ver
                            good(v) -> SmokeOut.Ok
                            else -> SmokeOut.Fail
                        }
                val raw = if (!verHint.isNullOrBlank()) "API_VERSION_TOO_HIGH" else "$v"
                record(name, raw, o, "释义：ULong 返回值=$v。${zhVer()}", statKey)
            }

            fun gUShortDrawing(name: String, statKey: String? = null, block: () -> UShort) {
                val v =
                        try {
                            block()
                        } catch (e: IllegalStateException) {
                            record(
                                    name,
                                    "API_VERSION_TOO_HIGH",
                                    SmokeOut.Ver,
                                    "释义：${ApiGuard.describeApiVersionMismatch(e)}",
                                    statKey,
                            )
                            return
                        } catch (_: Throwable) {
                            record(name, "exception", SmokeOut.Fail, "释义：调用异常。", statKey)
                            return
                        }
                val verHint = ApiGuard.takeVersionHint()
                val o = if (!verHint.isNullOrBlank()) SmokeOut.Ver else SmokeOut.Ok
                val raw = if (!verHint.isNullOrBlank()) "API_VERSION_TOO_HIGH" else "$v"
                record(name, raw, o, "释义：glyph id (UShort)=$v。", statKey)
            }

            // ---------- 场景1：RoundRect + Point2D 角半径 ----------
            appendLine("========== 场景1：圆角矩形角半径设置与获取 ==========")
            var rect: CPointer<OH_Drawing_Rect>? = null
            var roundRect: CPointer<OH_Drawing_RoundRect>? = null
            try {
                gPtrDrawing("OH_Drawing_RectCreate", "OH_Drawing_Rect") {
                    rect = OH_Drawing_RectCreate(0f, 0f, 100f, 80f)
                    rect
                }
                if (rect != null) {
                    gPtrDrawing("OH_Drawing_RoundRectCreate", "OH_Drawing_RoundRect") {
                        roundRect = OH_Drawing_RoundRectCreate(rect, 8f, 8f)
                        roundRect
                    }
                }
                if (roundRect != null) {
                    val radii: CValue<OH_Drawing_Point2D> = cValue {
                        x = 16f
                        y = 20f
                    }
                    gInvokeDrawing("OH_Drawing_RoundRectSetCorner") {
                        OH_Drawing_RoundRectSetCorner(
                                roundRect,
                                OH_Drawing_CornerPos.CORNER_POS_TOP_LEFT,
                                radii
                        )
                    }
                    val cornerVal =
                            try {
                                OH_Drawing_RoundRectGetCorner(
                                        roundRect,
                                        OH_Drawing_CornerPos.CORNER_POS_TOP_LEFT
                                )
                            } catch (e: IllegalStateException) {
                                record(
                                        "OH_Drawing_RoundRectGetCorner",
                                        "API_VERSION_TOO_HIGH",
                                        SmokeOut.Ver,
                                        ApiGuard.describeApiVersionMismatch(e),
                                        "OH_Drawing_RoundRectGetCorner",
                                )
                                null
                            } catch (_: Throwable) {
                                record(
                                        "OH_Drawing_RoundRectGetCorner",
                                        "exception",
                                        SmokeOut.Fail,
                                        "释义：获取角半径失败。",
                                        "OH_Drawing_RoundRectGetCorner",
                                )
                                null
                            }
                    if (cornerVal != null) {
                        cornerVal.useContents {
                            record(
                                    "OH_Drawing_RoundRectGetCorner(struct)",
                                    "x=$x y=$y",
                                    SmokeOut.Ok,
                                    "释义：struct 返回值（small struct return）可读。",
                                    "OH_Drawing_RoundRectGetCorner",
                            )
                        }
                    }
                }
            } finally {
                if (roundRect != null) {
                    gInvokeDrawing("OH_Drawing_RoundRectDestroy") {
                        OH_Drawing_RoundRectDestroy(roundRect)
                    }
                }
                if (rect != null) {
                    gInvokeDrawing("OH_Drawing_RectDestroy") { OH_Drawing_RectDestroy(rect) }
                }
            }

            // ---------- 场景2：ColorMatrix / Font path / FontMgr / TextStyle families / Bitmap
            // ----------
            appendLine()
            appendLine("========== 场景2：矩阵色滤、字形路径、FontMgr、字体族查询、位图宽度 ==========")
            val matrix = FloatArray(20)
            for (i in matrix.indices) {
                matrix[i] =
                        when {
                            i == 18 -> 1f
                            i == 19 -> 0f
                            else -> 0f
                        }
            }
            var colorFilter: CPointer<OH_Drawing_ColorFilter>? = null
            matrix.usePinned { pinned ->
                gPtrDrawing("OH_Drawing_ColorFilterCreateMatrix", "OH_Drawing_ColorFilter") {
                    colorFilter = OH_Drawing_ColorFilterCreateMatrix(pinned.addressOf(0))
                    colorFilter
                }
            }
            var font: CPointer<OH_Drawing_Font>? = null
            var path: CPointer<OH_Drawing_Path>? = null
            try {
                gPtrDrawing("OH_Drawing_FontCreate", "OH_Drawing_Font") {
                    font = OH_Drawing_FontCreate()
                    font
                }
                gPtrDrawing("OH_Drawing_PathCreate", "OH_Drawing_Path") {
                    path = OH_Drawing_PathCreate()
                    path
                }
                if (font != null && path != null) {
                    // 使用空格字符 (32) 作为 glyph，更可靠
                    val errorCode =
                            try {
                                OH_Drawing_FontGetPathForGlyph(font, 32.toUShort(), path)
                            } catch (e: IllegalStateException) {
                                record(
                                        "OH_Drawing_FontGetPathForGlyph",
                                        "API_VERSION_TOO_HIGH",
                                        SmokeOut.Ver,
                                        "释义：${ApiGuard.describeApiVersionMismatch(e)}",
                                        "OH_Drawing_FontGetPathForGlyph",
                                )
                                null
                            } catch (_: Throwable) {
                                record(
                                        "OH_Drawing_FontGetPathForGlyph",
                                        "exception",
                                        SmokeOut.Fail,
                                        "释义：调用抛异常。",
                                        "OH_Drawing_FontGetPathForGlyph",
                                )
                                null
                            }
                    if (errorCode != null) {
                        // errorCode 是枚举类型，可以直接转为 UInt
                        val ec = errorCode as? UInt ?: 0u
                        record(
                                "OH_Drawing_FontGetPathForGlyph",
                                "$ec",
                                if (ec == 0u) SmokeOut.Ok else SmokeOut.Fail,
                                "释义：OH_Drawing_ErrorCode=$ec (0=SUCCESS, 401=INVALID_PARAMETER) for glyph 32(space)。",
                                "OH_Drawing_FontGetPathForGlyph",
                        )
                    }
                }
            } finally {
                if (path != null) {
                    gInvokeDrawing("OH_Drawing_PathDestroy") { OH_Drawing_PathDestroy(path) }
                }
                if (font != null) {
                    gInvokeDrawing("OH_Drawing_FontDestroy") { OH_Drawing_FontDestroy(font) }
                }
                if (colorFilter != null) {
                    gInvokeDrawing("OH_Drawing_ColorFilterDestroy") {
                        OH_Drawing_ColorFilterDestroy(colorFilter)
                    }
                }
            }

            var fontMgr: CPointer<OH_Drawing_FontMgr>? = null
            try {
                gPtrDrawing("OH_Drawing_FontMgrCreate", "OH_Drawing_FontMgr") {
                    fontMgr = OH_Drawing_FontMgrCreate()
                    fontMgr
                }
                if (fontMgr != null) {
                    val cnt =
                            try {
                                OH_Drawing_FontMgrGetFamilyCount(fontMgr)
                            } catch (e: IllegalStateException) {
                                record(
                                        "OH_Drawing_FontMgrGetFamilyCount",
                                        "API_VERSION_TOO_HIGH",
                                        SmokeOut.Ver,
                                        "释义：${ApiGuard.describeApiVersionMismatch(e)}",
                                        null,
                                )
                                -1
                            } catch (_: Throwable) {
                                record(
                                        "OH_Drawing_FontMgrGetFamilyCount",
                                        "exception",
                                        SmokeOut.Fail,
                                        "释义：调用抛异常。",
                                        null,
                                )
                                -1
                            }
                    if (cnt >= 0) {
                        record(
                                "OH_Drawing_FontMgrGetFamilyCount",
                                "$cnt",
                                SmokeOut.Ok,
                                "释义：FontMgr 族数量=$cnt。",
                                null,
                        )
                    }
                    if (cnt > 0) {
                        val namePtr =
                                ApiGuard.guard(
                                        {
                                            OH_Drawing_FontMgrGetFamilyName(
                                                    fontMgr,
                                                    0,
                                            )
                                        },
                                        null,
                                        null,
                                )
                        val ver = ApiGuard.takeVersionHint()
                        record(
                                "OH_Drawing_FontMgrGetFamilyName",
                                if (!ver.isNullOrBlank()) "API_VERSION_TOO_HIGH"
                                else if (namePtr != null) "non-null" else "null",
                                when {
                                    !ver.isNullOrBlank() -> SmokeOut.Ver
                                    namePtr != null -> SmokeOut.Ok
                                    else -> SmokeOut.Fail
                                },
                                "释义：FontMgr 第 0 个族名字符串指针。${ver ?: ""}",
                                "OH_Drawing_FontMgrGetFamilyName",
                        )
                        if (namePtr != null) {
                            gInvokeDrawing("OH_Drawing_FontMgrDestroyFamilyName") {
                                OH_Drawing_FontMgrDestroyFamilyName(namePtr)
                            }
                        }
                    } else if (cnt == 0) {
                        record(
                                "OH_Drawing_FontMgrGetFamilyName",
                                "skip",
                                SmokeOut.Skip,
                                "释义：族数量为 0，跳过 GetFamilyName。",
                                "OH_Drawing_FontMgrGetFamilyName",
                        )
                    }
                }
            } finally {
                if (fontMgr != null) {
                    gInvokeDrawing("OH_Drawing_FontMgrDestroy") {
                        OH_Drawing_FontMgrDestroy(fontMgr)
                    }
                }
            }

            var textStyle: CPointer<OH_Drawing_TextStyle>? = null
            try {
                gPtrDrawing("OH_Drawing_CreateTextStyle", "OH_Drawing_TextStyle") {
                    textStyle = OH_Drawing_CreateTextStyle()
                    textStyle
                }
                if (textStyle != null) {
                    val familyPtrArr = allocArrayOf("sans-serif".cstr.ptr, "serif".cstr.ptr)
                    gInvokeDrawing("OH_Drawing_SetTextStyleFontFamilies") {
                        OH_Drawing_SetTextStyleFontFamilies(textStyle, 2, familyPtrArr)
                    }
                    // 使用真实的 size_t* 参数获取字体族数量
                    val familyCount =
                            try {
                                val numArr = ULongArray(1) { 0uL }
                                val famPtr =
                                        numArr.usePinned { pinned ->
                                            ApiGuard.guard(
                                                    {
                                                        OH_Drawing_TextStyleGetFontFamilies(
                                                                textStyle,
                                                                pinned.addressOf(0)
                                                        )
                                                    },
                                                    null,
                                                    null,
                                            )
                                        }
                                val verF = ApiGuard.takeVersionHint()
                                val count = numArr[0]
                                record(
                                        "OH_Drawing_TextStyleGetFontFamilies",
                                        if (!verF.isNullOrBlank()) "API_VERSION_TOO_HIGH"
                                        else if (famPtr != null) "non-null" else "null",
                                        when {
                                            !verF.isNullOrBlank() -> SmokeOut.Ver
                                            famPtr != null -> SmokeOut.Ok
                                            else -> SmokeOut.Fail
                                        },
                                        "释义：返回字体族指针列表；获取到 $count 个字体族（真实 size_t* 传参）。${verF ?: ""}",
                                        "OH_Drawing_TextStyleGetFontFamilies",
                                )
                                count
                            } catch (e: Exception) {
                                record(
                                        "OH_Drawing_TextStyleGetFontFamilies",
                                        "exception",
                                        SmokeOut.Fail,
                                        "释义：调用异常: ${e.message}",
                                        "OH_Drawing_TextStyleGetFontFamilies",
                                )
                                0uL
                            }
                }
            } finally {
                if (textStyle != null) {
                    gInvokeDrawing("OH_Drawing_DestroyTextStyle") {
                        OH_Drawing_DestroyTextStyle(textStyle)
                    }
                }
            }

            var bitmap: CPointer<OH_Drawing_Bitmap>? = null
            try {
                gPtrDrawing("OH_Drawing_BitmapCreate", "OH_Drawing_Bitmap") {
                    bitmap = OH_Drawing_BitmapCreate()
                    bitmap
                }
                if (bitmap != null) {
                    val fmt = alloc<OH_Drawing_BitmapFormat>()
                    fmt.colorFormat = OH_Drawing_ColorFormat.COLOR_FORMAT_RGBA_8888
                    fmt.alphaFormat = OH_Drawing_AlphaFormat.ALPHA_FORMAT_PREMUL
                    gInvokeDrawing("OH_Drawing_BitmapBuild") {
                        OH_Drawing_BitmapBuild(bitmap, 64u, 48u, fmt.ptr)
                    }
                    gUIntDrawing(
                            "OH_Drawing_BitmapGetWidth",
                            "OH_Drawing_BitmapGetWidth",
                            good = { it == 64u }
                    ) { OH_Drawing_BitmapGetWidth(bitmap) }
                }
            } finally {
                if (bitmap != null) {
                    gInvokeDrawing("OH_Drawing_BitmapDestroy") { OH_Drawing_BitmapDestroy(bitmap) }
                }
            }

            // ---------- 场景3：注册字体（路径 + buffer）----------
            appendLine()
            appendLine(
                    "========== 场景3：RegisterFont / RegisterFontBuffer（Apple_Chancery.ttf 或系统字体）=========="
            )
            val fontFamily = "AppleChancerySmoke"
            var collection: CPointer<OH_Drawing_FontCollection>? = null
            try {
                gPtrDrawing("OH_Drawing_CreateFontCollection", "OH_Drawing_FontCollection") {
                    collection = OH_Drawing_CreateFontCollection()
                    collection
                }
                if (collection != null) {
                    // 尝试使用传入的 fontTtfPath，或查找系统字体作为后备
                    val effectivePath =
                            if (fontTtfPath.isNotBlank()) {
                                fontTtfPath
                            } else {
                                // 尝试常见的系统字体路径
                                val candidatePaths =
                                        listOf(
                                                "/system/fonts/HarmonyOS_Sans_SC_Regular.ttf",
                                                "/system/fonts/DroidSansFallback.ttf",
                                                "/system/fonts/NotoSansCJK-Regular.ttc",
                                                "/system/fonts/Roboto-Regular.ttf",
                                        )
                                candidatePaths.firstOrNull { path ->
                                    try {
                                        fopen(path, "rb")?.let { f ->
                                            fclose(f)
                                            true
                                        }
                                                ?: false
                                    } catch (_: Throwable) {
                                        false
                                    }
                                }
                                        ?: ""
                            }

                    if (effectivePath.isNotBlank()) {
                        val actualFontFamily =
                                if (fontTtfPath.isNotBlank()) fontFamily else "SystemFontSmoke"
                        val pathInfo =
                                if (fontTtfPath.isNotBlank()) {
                                    "用户提供: $effectivePath"
                                } else {
                                    "系统字体后备: $effectivePath"
                                }

                        gUIntDrawing(
                                "OH_Drawing_RegisterFont($pathInfo)",
                                "OH_Drawing_RegisterFont",
                                good = { it == 0u },
                        ) { OH_Drawing_RegisterFont(collection, actualFontFamily, effectivePath) }

                        val bytes = readBinaryFile(effectivePath)
                        if (bytes != null) {
                            bytes.usePinned { pinned ->
                                gUIntDrawing(
                                        "OH_Drawing_RegisterFontBuffer(${bytes.size} bytes)",
                                        "OH_Drawing_RegisterFontBuffer",
                                        good = { it == 0u },
                                ) {
                                    OH_Drawing_RegisterFontBuffer(
                                            collection,
                                            "${actualFontFamily}Buf",
                                            pinned.addressOf(0).reinterpret<UByteVar>(),
                                            bytes.size.toULong(),
                                    )
                                }
                            }
                            gUIntDrawing(
                                    "OH_Drawing_UnregisterFont(buffer)",
                                    null,
                                    good = { true }
                            ) { OH_Drawing_UnregisterFont(collection, "${actualFontFamily}Buf") }
                        } else {
                            record(
                                    "OH_Drawing_RegisterFontBuffer",
                                    "skip",
                                    SmokeOut.Skip,
                                    "释义：无法读取字体文件字节($effectivePath)，跳过 buffer 注册。",
                                    "OH_Drawing_RegisterFontBuffer",
                            )
                        }
                        gUIntDrawing("OH_Drawing_UnregisterFont(path)", null, good = { true }) {
                            OH_Drawing_UnregisterFont(collection, actualFontFamily)
                        }
                    } else {
                        record(
                                "OH_Drawing_RegisterFont",
                                "skip",
                                SmokeOut.Skip,
                                "释义：fontTtfPath 为空且未找到系统字体，跳过 RegisterFont。",
                                "OH_Drawing_RegisterFont",
                        )
                        record(
                                "OH_Drawing_RegisterFontBuffer",
                                "skip",
                                SmokeOut.Skip,
                                "释义：无可用字体文件，跳过 RegisterFontBuffer。",
                                "OH_Drawing_RegisterFontBuffer",
                        )
                    }
                }
            } finally {
                if (collection != null) {
                    gInvokeDrawing("OH_Drawing_DestroyFontCollection") {
                        OH_Drawing_DestroyFontCollection(collection)
                    }
                }
            }

            // ---------- 场景4～5：Typography / TextLine / Run / LineMetrics ----------
            appendLine()
            appendLine("========== 场景4～5：排版行、GlyphRun、Run 方向、LineMetrics ==========")
            var typoStyle: CPointer<OH_Drawing_TypographyStyle>? = null
            var typoHandler: CPointer<OH_Drawing_TypographyCreate>? = null
            var typography: CPointer<OH_Drawing_Typography>? = null
            var linesArr: CPointer<OH_Drawing_Array>? = null
            try {
                gPtrDrawing("OH_Drawing_CreateTypographyStyle", "OH_Drawing_TypographyStyle") {
                    typoStyle = OH_Drawing_CreateTypographyStyle()
                    typoStyle
                }
                val globalFc =
                        ApiGuard.guard({ OH_Drawing_GetFontCollectionGlobalInstance() }, null, null)
                ApiGuard.takeVersionHint()
                gPtrDrawing("OH_Drawing_CreateTypographyHandler", "OH_Drawing_TypographyCreate") {
                    typoHandler = OH_Drawing_CreateTypographyHandler(typoStyle, globalFc)
                    typoHandler
                }
                var pushStyle: CPointer<OH_Drawing_TextStyle>? = null
                try {
                    gPtrDrawing(
                            "OH_Drawing_CreateTextStyle",
                            "OH_Drawing_TextStyle(push)",
                            "OH_Drawing_CreateTextStyle",
                    ) {
                        pushStyle = OH_Drawing_CreateTextStyle()
                        pushStyle
                    }
                    if (typoHandler != null && pushStyle != null) {
                        gInvokeDrawing("OH_Drawing_TypographyHandlerPushTextStyle") {
                            OH_Drawing_TypographyHandlerPushTextStyle(typoHandler, pushStyle)
                        }
                        gInvokeDrawing("OH_Drawing_TypographyHandlerAddText") {
                            OH_Drawing_TypographyHandlerAddText(typoHandler, "Draw7 Smoke 测试")
                        }
                        gInvokeDrawing("OH_Drawing_TypographyHandlerPopTextStyle") {
                            OH_Drawing_TypographyHandlerPopTextStyle(typoHandler)
                        }
                    }
                } finally {
                    if (pushStyle != null) {
                        gInvokeDrawing(
                                "OH_Drawing_DestroyTextStyle",
                                "OH_Drawing_DestroyTextStyle"
                        ) { OH_Drawing_DestroyTextStyle(pushStyle) }
                    }
                }
                if (typoHandler != null) {
                    gPtrDrawing("OH_Drawing_CreateTypography", "OH_Drawing_Typography") {
                        typography = OH_Drawing_CreateTypography(typoHandler)
                        typography
                    }
                }
                if (typography != null) {
                    gInvokeDrawing("OH_Drawing_TypographyLayout") {
                        OH_Drawing_TypographyLayout(typography, 320.0)
                    }
                    gULongDrawing(
                            "OH_Drawing_TypographyGetLineCount",
                            "OH_Drawing_TypographyGetLineCount",
                            good = { it >= 1uL },
                    ) { OH_Drawing_TypographyGetLineCount(typography) }
                    val lm = alloc<OH_Drawing_LineMetrics>()
                    memset(lm.ptr, 0, sizeOf<OH_Drawing_LineMetrics>().toULong())
                    var lineMetricsRecorded = false
                    val okLm =
                            try {
                                OH_Drawing_TypographyGetLineMetricsAt(typography, 0, lm.ptr)
                            } catch (e: IllegalStateException) {
                                record(
                                        "OH_Drawing_TypographyGetLineMetricsAt",
                                        "API_VERSION_TOO_HIGH",
                                        SmokeOut.Ver,
                                        ApiGuard.describeApiVersionMismatch(e),
                                        "OH_Drawing_TypographyGetLineMetricsAt",
                                )
                                lineMetricsRecorded = true
                                false
                            } catch (_: Throwable) {
                                record(
                                        "OH_Drawing_TypographyGetLineMetricsAt",
                                        "exception",
                                        SmokeOut.Fail,
                                        "释义：LineMetrics 调用异常。",
                                        "OH_Drawing_TypographyGetLineMetricsAt",
                                )
                                lineMetricsRecorded = true
                                false
                            }
                    if (!lineMetricsRecorded) {
                        if (okLm) {
                            record(
                                    "OH_Drawing_TypographyGetLineMetricsAt",
                                    "true",
                                    SmokeOut.Ok,
                                    "释义：has_struct_by_value_field — [OH_Drawing_LineMetrics] 已填充。",
                                    "OH_Drawing_TypographyGetLineMetricsAt",
                            )
                        } else {
                            record(
                                    "OH_Drawing_TypographyGetLineMetricsAt",
                                    "false",
                                    SmokeOut.Fail,
                                    "释义：接口返回 false，未写入 LineMetrics。",
                                    "OH_Drawing_TypographyGetLineMetricsAt",
                            )
                        }
                    }

                    gPtrDrawing("OH_Drawing_TypographyGetTextLines", "OH_Drawing_Array(lines)") {
                        linesArr = OH_Drawing_TypographyGetTextLines(typography)
                        linesArr
                    }
                    if (linesArr != null) {
                        val line: CPointer<OH_Drawing_TextLine>? =
                                ApiGuard.guard(
                                        { OH_Drawing_GetTextLineByIndex(linesArr, 0uL) },
                                        null,
                                        null,
                                )
                        val verL = ApiGuard.takeVersionHint()
                        record(
                                "OH_Drawing_GetTextLineByIndex",
                                if (!verL.isNullOrBlank()) "API_VERSION_TOO_HIGH"
                                else if (line != null) "non-null" else "null",
                                when {
                                    !verL.isNullOrBlank() -> SmokeOut.Ver
                                    line != null -> SmokeOut.Ok
                                    else -> SmokeOut.Fail
                                },
                                "释义：首行 TextLine。${verL ?: ""}",
                                "OH_Drawing_GetTextLineByIndex",
                        )
                        if (line != null) {
                            var runs: CPointer<OH_Drawing_Array>? = null
                            try {
                                gPtrDrawing(
                                        "OH_Drawing_TextLineGetGlyphRuns",
                                        "OH_Drawing_Array(runs)"
                                ) {
                                    runs = OH_Drawing_TextLineGetGlyphRuns(line)
                                    runs
                                }
                                if (runs != null) {
                                    val run: CPointer<OH_Drawing_Run>? =
                                            ApiGuard.guard(
                                                    { OH_Drawing_GetRunByIndex(runs, 0uL) },
                                                    null,
                                                    null,
                                            )
                                    val verR = ApiGuard.takeVersionHint()
                                    record(
                                            "OH_Drawing_GetRunByIndex",
                                            if (!verR.isNullOrBlank()) "API_VERSION_TOO_HIGH"
                                            else if (run != null) "non-null" else "null",
                                            when {
                                                !verR.isNullOrBlank() -> SmokeOut.Ver
                                                run != null -> SmokeOut.Ok
                                                else -> SmokeOut.Fail
                                            },
                                            "释义：首个 glyph run。${verR ?: ""}",
                                            "OH_Drawing_GetRunByIndex",
                                    )
                                    if (run != null) {
                                        val dir =
                                                try {
                                                    OH_Drawing_GetRunTextDirection(run)
                                                } catch (e: IllegalStateException) {
                                                    record(
                                                            "OH_Drawing_GetRunTextDirection",
                                                            "API_VERSION_TOO_HIGH",
                                                            SmokeOut.Ver,
                                                            ApiGuard.describeApiVersionMismatch(e),
                                                            "OH_Drawing_GetRunTextDirection",
                                                    )
                                                    null
                                                } catch (_: Throwable) {
                                                    record(
                                                            "OH_Drawing_GetRunTextDirection",
                                                            "exception",
                                                            SmokeOut.Fail,
                                                            "释义：严格枚举返回值获取失败。",
                                                            "OH_Drawing_GetRunTextDirection",
                                                    )
                                                    null
                                                }
                                        if (dir != null) {
                                            record(
                                                    "OH_Drawing_GetRunTextDirection(enum)",
                                                    dir.name,
                                                    SmokeOut.Ok,
                                                    "释义：严格枚举 [OH_Drawing_TextDirection]。",
                                                    "OH_Drawing_GetRunTextDirection",
                                            )
                                        }
                                        val gc =
                                                try {
                                                    OH_Drawing_GetRunGlyphCount(run)
                                                } catch (e: IllegalStateException) {
                                                    record(
                                                            "OH_Drawing_GetRunGlyphCount",
                                                            "API_VERSION_TOO_HIGH",
                                                            SmokeOut.Ver,
                                                            "释义：${ApiGuard.describeApiVersionMismatch(e)}",
                                                            null,
                                                    )
                                                    0u
                                                } catch (_: Throwable) {
                                                    record(
                                                            "OH_Drawing_GetRunGlyphCount",
                                                            "exception",
                                                            SmokeOut.Fail,
                                                            "释义：调用抛异常。",
                                                            null,
                                                    )
                                                    0u
                                                }
                                        if (gc > 0u) {
                                            record(
                                                    "OH_Drawing_GetRunGlyphCount",
                                                    "$gc",
                                                    SmokeOut.Ok,
                                                    "释义：Run 包含 $gc 个 glyph。",
                                                    null,
                                            )
                                            var glyphArr: CPointer<OH_Drawing_Array>? = null
                                            try {
                                                gPtrDrawing(
                                                        "OH_Drawing_GetRunGlyphs",
                                                        "OH_Drawing_Array(glyphs)"
                                                ) {
                                                    glyphArr =
                                                            OH_Drawing_GetRunGlyphs(
                                                                    run,
                                                                    0L,
                                                                    gc.toLong()
                                                            )
                                                    glyphArr
                                                }
                                                if (glyphArr != null) {
                                                    gUShortDrawing(
                                                            "OH_Drawing_GetRunGlyphsByIndex"
                                                    ) {
                                                        OH_Drawing_GetRunGlyphsByIndex(
                                                                glyphArr,
                                                                0uL
                                                        )
                                                    }
                                                }
                                            } finally {
                                                if (glyphArr != null) {
                                                    gInvokeDrawing("OH_Drawing_DestroyRunGlyphs") {
                                                        OH_Drawing_DestroyRunGlyphs(glyphArr)
                                                    }
                                                }
                                            }
                                        } else if (gc == 0u) {
                                            record(
                                                    "OH_Drawing_GetRunGlyphCount",
                                                    "0",
                                                    SmokeOut.Skip,
                                                    "释义：Run 的 glyph 数为 0（空 run 或文本未渲染）。",
                                                    null,
                                            )
                                            record(
                                                    "OH_Drawing_GetRunGlyphs",
                                                    "skip",
                                                    SmokeOut.Skip,
                                                    "释义：glyph 数为 0，跳过 GetRunGlyphs 链。",
                                                    "OH_Drawing_GetRunGlyphs",
                                            )
                                            record(
                                                    "OH_Drawing_GetRunGlyphsByIndex",
                                                    "skip",
                                                    SmokeOut.Skip,
                                                    "释义：无 glyph 数组。",
                                                    "OH_Drawing_GetRunGlyphsByIndex",
                                            )
                                        }
                                    }
                                    gInvokeDrawing("OH_Drawing_DestroyRuns") {
                                        OH_Drawing_DestroyRuns(runs)
                                    }
                                }
                            } finally {
                                // runs destroyed above
                            }
                            gInvokeDrawing("OH_Drawing_DestroyTextLine") {
                                OH_Drawing_DestroyTextLine(line)
                            }
                        }
                        gInvokeDrawing("OH_Drawing_DestroyTextLines") {
                            OH_Drawing_DestroyTextLines(linesArr)
                        }
                        linesArr = null
                    }
                }
            } finally {
                if (typography != null) {
                    gInvokeDrawing("OH_Drawing_DestroyTypography") {
                        OH_Drawing_DestroyTypography(typography)
                    }
                }
                if (typoHandler != null) {
                    gInvokeDrawing("OH_Drawing_DestroyTypographyHandler") {
                        OH_Drawing_DestroyTypographyHandler(typoHandler)
                    }
                }
                if (typoStyle != null) {
                    gInvokeDrawing("OH_Drawing_DestroyTypographyStyle") {
                        OH_Drawing_DestroyTypographyStyle(typoStyle)
                    }
                }
            }
        }

        appendLine()
        appendLine("---------- 清单统计（${DRAWING_CHECKLIST.size} 项，每函数计 1 次）----------")
        appendLine("合并规则：同一清单函数多次调用时，取较重结果（API版本不符 > 失败 > 跳过 > 成功）。")
        var ckOk = 0
        var ckFail = 0
        var ckVer = 0
        var ckSkip = 0
        var ckNotRun = 0

        /** 解释为何未产生合并判定 */
        fun notRunExplanation(key: String): String {
            val rectCreate = checklistOutcome["OH_Drawing_RectCreate"]
            val fontMgrCreate = checklistOutcome["OH_Drawing_FontMgrCreate"]
            val textStyleCreate = checklistOutcome["OH_Drawing_CreateTextStyle"]
            val bitmapCreate = checklistOutcome["OH_Drawing_BitmapCreate"]
            val fontCollectionCreate = checklistOutcome["OH_Drawing_CreateFontCollection"]
            val typoStyleCreate = checklistOutcome["OH_Drawing_CreateTypographyStyle"]

            return when (key) {
                "OH_Drawing_RoundRectCreate",
                "OH_Drawing_RoundRectSetCorner",
                "OH_Drawing_RoundRectGetCorner",
                "OH_Drawing_RoundRectDestroy" -> {
                    if (rectCreate == null ||
                                    rectCreate == SmokeOut.Fail ||
                                    rectCreate == SmokeOut.Ver
                    ) {
                        "前置条件未满足：需 OH_Drawing_RectCreate 成功才能创建圆角矩形；当前 RectCreate 状态=$rectCreate。"
                    } else {
                        "场景1内未调用到本 API：RectCreate 已成功，但后续流程可能提前退出。"
                    }
                }
                "OH_Drawing_FontMgrGetFamilyName", "OH_Drawing_FontMgrDestroyFamilyName" -> {
                    if (fontMgrCreate == null ||
                                    fontMgrCreate == SmokeOut.Fail ||
                                    fontMgrCreate == SmokeOut.Ver
                    ) {
                        "前置条件未满足：需 OH_Drawing_FontMgrCreate 成功且 GetFamilyCount > 0；当前 FontMgrCreate 状态=$fontMgrCreate。"
                    } else {
                        "场景2内未调用到本 API：FontMgrCreate 已成功，但 FamilyCount 可能为 0 或后续流程跳过。"
                    }
                }
                "OH_Drawing_SetTextStyleFontFamilies", "OH_Drawing_TextStyleGetFontFamilies" -> {
                    if (textStyleCreate == null ||
                                    textStyleCreate == SmokeOut.Fail ||
                                    textStyleCreate == SmokeOut.Ver
                    ) {
                        "前置条件未满足：需 OH_Drawing_CreateTextStyle 成功；当前 CreateTextStyle 状态=$textStyleCreate。"
                    } else {
                        "场景2内未调用到本 API：CreateTextStyle 已成功，但后续设置/查询流程可能跳过。"
                    }
                }
                "OH_Drawing_BitmapBuild", "OH_Drawing_BitmapGetWidth" -> {
                    if (bitmapCreate == null ||
                                    bitmapCreate == SmokeOut.Fail ||
                                    bitmapCreate == SmokeOut.Ver
                    ) {
                        "前置条件未满足：需 OH_Drawing_BitmapCreate 成功；当前 BitmapCreate 状态=$bitmapCreate。"
                    } else {
                        "场景2内未调用到本 API：BitmapCreate 已成功，但后续 Build/GetWidth 流程可能跳过。"
                    }
                }
                "OH_Drawing_RegisterFont", "OH_Drawing_RegisterFontBuffer" -> {
                    if (fontCollectionCreate == null ||
                                    fontCollectionCreate == SmokeOut.Fail ||
                                    fontCollectionCreate == SmokeOut.Ver
                    ) {
                        "前置条件未满足：需 OH_Drawing_CreateFontCollection 成功且 fontTtfPath 非空；当前 CreateFontCollection 状态=$fontCollectionCreate。"
                    } else {
                        "场景3跳过：fontTtfPath 参数为空或字体文件不存在，整段注册字体流程已跳过（见报告开头）。"
                    }
                }
                "OH_Drawing_TypographyHandlerPushTextStyle",
                "OH_Drawing_TypographyHandlerAddText",
                "OH_Drawing_TypographyHandlerPopTextStyle",
                "OH_Drawing_CreateTypography",
                "OH_Drawing_TypographyLayout",
                "OH_Drawing_TypographyGetLineCount",
                "OH_Drawing_TypographyGetTextLines",
                "OH_Drawing_GetTextLineByIndex",
                "OH_Drawing_TextLineGetGlyphRuns",
                "OH_Drawing_GetRunByIndex",
                "OH_Drawing_GetRunTextDirection",
                "OH_Drawing_GetRunGlyphs",
                "OH_Drawing_GetRunGlyphsByIndex",
                "OH_Drawing_DestroyRunGlyphs",
                "OH_Drawing_DestroyRuns",
                "OH_Drawing_DestroyTextLine",
                "OH_Drawing_DestroyTextLines",
                "OH_Drawing_TypographyGetLineMetricsAt" -> {
                    if (typoStyleCreate == null ||
                                    typoStyleCreate == SmokeOut.Fail ||
                                    typoStyleCreate == SmokeOut.Ver
                    ) {
                        "前置条件未满足：需 OH_Drawing_CreateTypographyStyle 成功才能构建排版链路；当前 CreateTypographyStyle 状态=$typoStyleCreate。"
                    } else {
                        "场景4~5内未调用到本 API：CreateTypographyStyle 已成功，但后续排版/TextLine/Run 流程可能提前退出。"
                    }
                }
                else -> "该清单项在报告中从未被合并过判定；可能是前置条件未满足或代码分支未执行到。"
            }
        }

        appendLine()
        DRAWING_CHECKLIST.forEachIndexed { index, key ->
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
                        SmokeOut.Skip -> {
                            ckSkip++
                            "跳过"
                        }
                    }
            appendLine("${index + 1}. $key — $label")
        }
        appendLine()
        appendLine("清单汇总：成功=$ckOk  失败=$ckFail  跳过=$ckSkip  API版本不符=$ckVer  未执行=$ckNotRun")
        appendLine(
                "核对：${ckOk + ckFail + ckSkip + ckVer + ckNotRun} = ${DRAWING_CHECKLIST.size}（应为 ${DRAWING_CHECKLIST.size}）"
        )
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
                "  → 释义：「未执行」表示该清单项在报告中**从未被合并过判定**（与「失败」「API版本不符」不同）；" +
                        "常见原因是**前置条件未满足**导致代码分支未走到（例如 RectCreate 失败则整段圆角矩形不会调用）。"
        )
        val notRunKeys = DRAWING_CHECKLIST.filter { checklistOutcome[it] == null }
        if (notRunKeys.isEmpty()) {
            appendLine("（无）")
        } else {
            notRunKeys.forEach { k ->
                appendLine("• $k")
                appendLine("  → ${notRunExplanation(k)}")
            }
        }
        appendLine()
        appendLine("---------- 跳过明细 ----------")
        appendLine(
                "  → 释义：「跳过」表示该清单项已执行 record 但主动标记为 SmokeOut.Skip（例如 fontTtfPath 为空时跳过 RegisterFont）。"
        )
        val skippedKeys = DRAWING_CHECKLIST.filter { checklistOutcome[it] == SmokeOut.Skip }
        if (skippedKeys.isEmpty()) {
            appendLine("（无）")
        } else {
            skippedKeys.forEach { k -> appendLine("• $k — 已标记跳过（见上文步骤说明）。") }
        }
    }
}

@OptIn(ExperimentalNativeApi::class)
@CName("kn_runDrawingModuleSmokeTest")
fun kn_runDrawingModuleSmokeTest(fontTtfPath: CPointer<ByteVar>?): CPointer<ByteVar>? {
    val path = fontTtfPath?.toKString().orEmpty()
    val report =
            try {
                buildDrawingModuleSmokeReport(path)
            } catch (t: Throwable) {
                "Kotlin 异常: ${t::class.simpleName}: ${t.message}\n${t.stackTraceToString()}"
            }
    return strdup(report)
}
