package com.tencent.tmm.knoi.callback

import com.google.devtools.ksp.symbol.KSType
import com.tencent.tmm.knoi.function.FunctionInfo

data class CallbackInfo(
    val clazzName: KSType,
    val packageName: String,
    val functionList: List<FunctionInfo>,
)
