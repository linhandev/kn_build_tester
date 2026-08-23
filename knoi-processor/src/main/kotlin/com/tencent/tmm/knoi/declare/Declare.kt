package com.tencent.tmm.knoi.declare

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType

fun getDeclareNameWithType(declareWithType: String): String {
    val index = declareWithType.indexOf(":")
    return if (index == -1) {
        declareWithType
    } else {
        declareWithType.substring(0, index)
    }
}

enum class DeclareType(type: String) {
    CONST("const")
}

data class Declare(
    val packageName: String,
    val clazz: KSDeclaration?,
    val field: String,
    val customName: String,
    val fieldType: KSType,
    val type: DeclareType = DeclareType.CONST
) {
    fun getDeclareName(): String {
        return "${packageName}${if (getClazzName().isEmpty()) "" else ".${getClazzName()}"}.${field}"
    }

    fun getDeclareNameWithType(): String {
        return getDeclareName() + ":${type}"
    }

    fun getShoreDeclareName(): String {
        return if (clazz == null) {
            field
        } else {
            val shortParentName = clazz.parentDeclaration?.qualifiedName?.getShortName()
            if (shortParentName == null) {
                return field
            } else {
                return "${shortParentName}.${field}"
            }
        }
    }

    fun getShortImportName(): String {
        return if (clazz == null) {
            field
        } else {
            clazz.parentDeclaration?.qualifiedName?.getShortName()!!
        }
    }

    private fun getClazzName(): String {
        return if (clazz == null) {
            ""
        } else {
            clazz.parentDeclaration?.qualifiedName?.getShortName()!!
        }
    }
}
