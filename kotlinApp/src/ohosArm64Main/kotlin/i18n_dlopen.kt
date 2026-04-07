@file:OptIn(
    kotlin.experimental.ExperimentalNativeApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
)

import i18n.tz.shim.TimeZoneRules
import platform.BasicServicesKit.DeviceInfo.OH_GetSdkApiVersion
import kotlin.native.CName
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CFunction
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.invoke
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import platform.posix.RTLD_NOW
import platform.posix.dlclose
import platform.posix.dlerror
import platform.posix.dlopen
import platform.posix.dlsym
import platform.posix.malloc
import platform.posix.memcpy
import platform.posix.memset

private const val REQUIRED_SDK_API: Int = 22
private const val LIB_OHI18N = "libohi18n.z.so"
private fun dlerrorString(): String = dlerror()?.toKString() ?: "unknown error"

fun getTimeZoneRulesViaI18n(): String {
    val api = OH_GetSdkApiVersion()
    if (api < REQUIRED_SDK_API) {
        return "API level $api is too low, need $REQUIRED_SDK_API"
    }

    val handle = dlopen(LIB_OHI18N, RTLD_NOW)
    if (handle == null) {
        return "dlopen($LIB_OHI18N) failed: ${dlerrorString()}"
    }
    try {
        val sym = dlsym(handle, "OH_i18n_GetTimeZoneRules")
        if (sym == null) {
            return "dlsym(OH_i18n_GetTimeZoneRules) failed: ${dlerrorString()}"
        }
        val getRulesFn: CPointer<CFunction<(CPointer<ByteVar>?, CPointer<TimeZoneRules>) -> Int>> =
            sym.reinterpret()
        val code = memScoped {
            val rules = alloc<TimeZoneRules>()
            memset(rules.ptr, 0, sizeOf<TimeZoneRules>().toULong())
            getRulesFn.invoke("Asia/Shanghai".cstr.ptr, rules.ptr)
        }
        return "OK, I18n_ErrorCode=$code"
    } finally {
        dlclose(handle)
    }
}

/**
 * UTF-8 NUL-terminated message for NAPI; allocated with [malloc] (leaked per call — fine for this demo).
 */
@CName("kn_getTimeZoneRulesViaI18n")
fun kn_getTimeZoneRulesViaI18n(): CPointer<ByteVar>? {
    val bytes = getTimeZoneRulesViaI18n().encodeToByteArray()
    val n = bytes.size + 1
    val raw = malloc(n.toULong())?.reinterpret<ByteVar>() ?: return null
    memset(raw, 0, n.toULong())
    if (bytes.isNotEmpty()) {
        bytes.usePinned { pinned ->
            memcpy(raw, pinned.addressOf(0), bytes.size.toULong())
        }
    }
    return raw
}
