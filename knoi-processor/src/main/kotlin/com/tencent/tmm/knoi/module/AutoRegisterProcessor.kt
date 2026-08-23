package com.tencent.tmm.knoi.module

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import com.tencent.tmm.knoi.annotation.ModuleInitializer
import com.tencent.tmm.knoi.utils.KNOI_MODULE_PACKAGE_NAME
import com.tencent.tmm.knoi.utils.OPTION_IS_BINARIES_MODULE
import com.tencent.tmm.knoi.utils.isAndroidApp
import com.tencent.tmm.knoi.utils.isJVM
import com.tencent.tmm.knoi.utils.isOhosArm64

@OptIn(KspExperimental::class)
fun processAutoRegister(
    codeGenerator: CodeGenerator,
    resolver: Resolver,
    options: Map<String, String>
) {
    if (!isBinariesModule(options) || isJVM(options) && !isAndroidApp(options)) {
        return
    }
    if (hasGenInitializeFile(resolver)) {
        return
    }
    val modulesSymbols = resolver.getDeclarationsFromPackage(KNOI_MODULE_PACKAGE_NAME)

    val modulesPackageTopFunctions =
        modulesSymbols.filter { it is KSFunctionDeclaration && it.validate() }
    val moduleInitializerList = modulesPackageTopFunctions.filter {
        it.annotations.find { methodAnnotation ->
            methodAnnotation.shortName.asString() == ModuleInitializer::class.simpleName
        } != null
    }.toList()
    val result = mutableListOf<KSAnnotated>()
    result.addAll(moduleInitializerList)

    genInitializeFun(codeGenerator, resolver, result,options)
}

private fun isBinariesModule(options: Map<String, String>): Boolean {
    val isBinariesModule = options[OPTION_IS_BINARIES_MODULE]
    if (isBinariesModule.isNullOrBlank() || !isBinariesModule.equals("true", true)) {
        return false
    }
    return true
}

fun genInitializeFun(
    codeGenerator: CodeGenerator,
    resolver: Resolver,
    modules: List<KSAnnotated>,
    options: Map<String, String>
) {
    val initializerFileSpecBuilder = FileSpec.builder("com.tencent.tmm.knoi", "Initializer")

    if (isOhosArm64(options)) {
        initializerFileSpecBuilder.addImport("kotlin.experimental", "ExperimentalNativeApi")
        initializerFileSpecBuilder.addImport("kotlinx.cinterop", "ExperimentalForeignApi")
    }
    val initializeFunSpec = FunSpec.builder("initialize")
    if (isOhosArm64(options)) {
        initializeFunSpec.addAnnotation(
            AnnotationSpec.builder(ClassName("kotlin", "OptIn")).addMember(
                "ExperimentalNativeApi::class"
            ).build()
        ).addAnnotation(
            AnnotationSpec.builder(ClassName("kotlin.native", "CName"))
                .addMember("externName = %S", "com_tencent_tmm_knoi_initBridge").build()
        )
    }
    val code =  """
        ${
        modules.map {
            if (it is KSFunctionDeclaration) {
                "|${it.packageName.asString()}.${it.qualifiedName!!.getShortName()}()"
            } else {
                ""
            }
        }.joinToString("\n")
    }
        |
        ${if (isOhosArm64(options)) "|initBridge()" else ""}
    """.trimMargin()
    initializeFunSpec.addCode(
       code
    )
    initializerFileSpecBuilder.addFunction(initializeFunSpec.build())
    if (isOhosArm64(options)) {
        genInitBridgeFunIfNeed(resolver, initializerFileSpecBuilder)
        genInitEnvFun(resolver, initializerFileSpecBuilder)
        genPreInitEnvFunIfNeed(resolver, initializerFileSpecBuilder)
    }
    val dependencies = Dependencies(
        true,
        *modules.filter { it.containingFile != null }.map { it.containingFile!! }.toTypedArray()
    )
    initializerFileSpecBuilder.build()
        .writeTo(codeGenerator = codeGenerator, dependencies = dependencies)
}

fun genInitEnvFun(resolver: Resolver, fileSpec: FileSpec.Builder) {
    val initEnvFun = FunSpec.builder("initEnvExport")
    initEnvFun.addParameter("env", ClassName("platform.ohos", "napi_env"))
    initEnvFun.addParameter("value", ClassName("platform.ohos", "napi_value"))
    initEnvFun.addParameter("debug", Boolean::class.asTypeName())
    initEnvFun.addAnnotation(
        AnnotationSpec.builder(ClassName("kotlin", "OptIn")).addMember(
            "ExperimentalForeignApi::class"
        ).addMember("ExperimentalNativeApi::class").build()
    ).addAnnotation(
        AnnotationSpec.builder(ClassName("kotlin.native", "CName"))
            .addMember("externName = %S", "com_tencent_tmm_knoi_initEnv").build()
    )
    initEnvFun.addCode(
        """
        preInitEnv(env, debug)
        InitEnv(env, value, debug)
    """.trimIndent()
    )
    fileSpec.addFunction(initEnvFun.build())
}

fun genInitBridgeFunIfNeed(resolver: Resolver, fileSpec: FileSpec.Builder) {
    val initBridge = resolver.getFunctionDeclarationsByName(
        resolver.getKSNameFromString("com.tencent.tmm.knoi.initBridge"), true
    )
    // 未自定义 initBridge 帮生成空方法
    if (initBridge.toList().isEmpty()) {
        fileSpec.addFunction(FunSpec.builder("initBridge").build())
    }
}

fun genPreInitEnvFunIfNeed(resolver: Resolver, fileSpec: FileSpec.Builder) {
    val initBridge = resolver.getFunctionDeclarationsByName(
        resolver.getKSNameFromString("com.tencent.tmm.knoi.preInitEnv"), true
    )
    // 未自定义 preInitEnv 帮生成空方法
    if (initBridge.toList().isEmpty()) {
        fileSpec.addFunction(
            FunSpec.builder("preInitEnv")
                .addParameter("env", ClassName("platform.ohos", "napi_env")).addParameter(
                    "debug", BOOLEAN
                ).addAnnotation(
                    AnnotationSpec.builder(ClassName("kotlin", "OptIn")).addMember(
                        "ExperimentalForeignApi::class"
                    ).addMember("ExperimentalNativeApi::class").build()
                ).build()
        )
    }
}

fun hasGenInitializeFile(resolver: Resolver): Boolean {
    val initialize = resolver.getFunctionDeclarationsByName(
        resolver.getKSNameFromString("com.tencent.tmm.knoi.initialize"), true
    )
    return initialize.toList().isNotEmpty()
}

