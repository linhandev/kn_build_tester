package com.tencent.tmm.knoi.service

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.tencent.tmm.knoi.function.FunctionInfo

data class ServiceInfo(
    val serviceName: String,
    val clazzName: KSType,
    val packageName: String,
    val functionList: List<FunctionInfo>,
    val declaration: KSClassDeclaration,
    val singleton: Boolean = false,
    val bind: KSType?
)
