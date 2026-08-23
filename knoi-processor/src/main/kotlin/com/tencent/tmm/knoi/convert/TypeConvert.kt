package com.tencent.tmm.knoi.convert

import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.UNIT

val classNameToJsType: Map<ClassName, String> = mapOf(
    UNIT to "void",
    BOOLEAN to "boolean",
    INT to "number",
    LONG to "number",
    DOUBLE to "number",
    STRING to "string",
    ARRAY to "Array",
    MAP to "object",
    ClassName("com.tencent.tmm.knoi.type", "ArrayBuffer") to "ArrayBuffer",
    ClassName("com.tencent.tmm.knoi.type", "JSValue") to "object"
)