package com.tencent.tmm.knoi.function

import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName

fun genProxyFunctionSpec(
    functionInfo: FunctionInfo,
    proxyFunction: String,
    extraParams: String = "",
    paramWrapper: (Param) -> String = { it.name },
    resultWrapper: (String, FunctionInfo, String) -> String = { _, _, result -> "return $result" }
): FunSpec {
    val func = FunSpec.builder(functionInfo.functionName)
    functionInfo.parameters.forEach {
        if (it.isVararg) {
            func.addParameter(it.name, it.type.toTypeName(), KModifier.VARARG)
        } else {
            func.addParameter(it.name, it.type.toTypeName())
        }
    }
    val type = if (functionInfo.returnType != null) {
        functionInfo.returnType.toTypeName()
    } else {
        UNIT
    }
    func.returns(type)
    func.addModifiers(Modifier.PUBLIC.toKModifier()!!, Modifier.OVERRIDE.toKModifier()!!)
    var paramsStr = ""
    functionInfo.parameters.forEachIndexed { index, param ->
        paramsStr += if (param.isVararg) {
            ", *${param.name}"
        } else {
            ", ${paramWrapper.invoke(param)}"
        }
    }
    func.addCode(
        """
        |${
            if (type == UNIT) {
                "${proxyFunction}<Unit>(${if(extraParams.isEmpty()) extraParams else "$extraParams,"}\"${functionInfo.functionName}\"${paramsStr})${if (type.isNullable) "" else "!!"}"
            } else {
                resultWrapper.invoke(
                    extraParams,
                    functionInfo,
                    "${proxyFunction}(${if(extraParams.isEmpty()) extraParams else "$extraParams,"}\"${functionInfo.functionName}\"${paramsStr})${if (type.isNullable) "" else "!!"}"
                )
            }
        }
        |""".trimMargin()
    )
    return func.build()
}