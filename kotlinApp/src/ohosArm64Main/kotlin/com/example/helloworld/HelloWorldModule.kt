@file:OptIn(ExperimentalNativeApi::class)

package com.example.helloworld

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.toKString
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.internal.ExportedBridge
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_env
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_value
import org.cpf.kotlin.akinterop.OhosFFIContext
import org.cpf.kotlin.akinterop.generated.registerKNExports
import kotlinx.coroutines.initMainHandler

@ExportedBridge("org_cpf_kotlin_akinterop_register")
fun registerAkInterop(
    env: napi_env,
    export: napi_value,
    bundleName: CPointer<ByteVar>,
    moduleName: CPointer<ByteVar>,
) {
    initMainHandler(env)
    OhosFFIContext.init(env, export, bundleName.toKString(), moduleName.toKString())
    registerKNExports(env, export)
}
