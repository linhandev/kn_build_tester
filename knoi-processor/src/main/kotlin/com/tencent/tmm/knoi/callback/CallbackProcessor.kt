package com.tencent.tmm.knoi.callback

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import com.tencent.tmm.knoi.annotation.KNCallback
import com.tencent.tmm.knoi.function.genProxyFunctionSpec
import com.tencent.tmm.knoi.function.parseFunctionsList
import com.tencent.tmm.knoi.utils.isOhosArm64
import com.tencent.tmm.knoi.tsgen.HEADER_TIPS
import com.tencent.tmm.knoi.tsgen.genTypeScriptInterface
import com.tencent.tmm.knoi.utils.OPTION_TYPESCRIPT_GEN_DIR
import java.io.File

val jsValueClazzName = ClassName("com.tencent.tmm.knoi.type", "JSValue")
fun processCallbackList(
    codeGenerator: CodeGenerator, resolver: Resolver, options: Map<String, String>
) {
    if (!isOhosArm64(options)){
        return
    }
    val callbackList = parseCallbackList(resolver)
    if (callbackList.isEmpty()) {
        return
    }
    genJSValueAsCallbackFunctions(codeGenerator, callbackList)
    genCallbackTypeScriptFile(callbackList, options)
}

fun genCallbackTypeScriptFile(callbackList: List<CallbackInfo>, options: Map<String, String>) {
    val genDir = options[OPTION_TYPESCRIPT_GEN_DIR]
    if (callbackList.isEmpty() || genDir.isNullOrEmpty()) {
        return
    }
    val scriptName = "callback.d.ts"
    val fileName = genDir + File.separator + scriptName
    val file = File(fileName)
    file.delete()
    file.parentFile.mkdirs()
    var fileContent = HEADER_TIPS
    callbackList.forEach {
        fileContent += genTypeScriptInterface(it.clazzName.toClassName().simpleName, it.functionList)
    }
    file.setWritable(true)
    file.writeText(fileContent)
}

fun genJSValueAsCallbackFunctions(codeGenerator: CodeGenerator, callbackList: List<CallbackInfo>) {
    if (callbackList.isEmpty()) {
        return
    }
    val packageNameToFileSpec = mutableMapOf<String, FileSpec.Builder>()


    callbackList.forEach {
        if (packageNameToFileSpec[it.packageName] == null) {
            packageNameToFileSpec[it.packageName] =
                FileSpec.builder(it.packageName, "CallbackExt")
        }
        val serviceConsumerFileSpecBuilder = packageNameToFileSpec[it.packageName] ?: return@forEach
        serviceConsumerFileSpecBuilder.addImport("com.tencent.tmm.knoi.type", "JSValue")
        serviceConsumerFileSpecBuilder.addType(genCallbackProxyClass(it))
        serviceConsumerFileSpecBuilder.addFunction(genJSValueAsCallbackFunction(it))
    }

    packageNameToFileSpec.forEach { (_, builder) ->
        builder.build().writeTo(codeGenerator = codeGenerator, aggregating = true)
    }
}

fun getCallbackProxyName(info: CallbackInfo) = info.clazzName.toClassName().simpleName + "Proxy"

fun genJSValueAsCallbackFunction(callbackInfo: CallbackInfo): FunSpec {
    val clazzName = callbackInfo.clazzName.toClassName().simpleName

    val func = FunSpec.builder("as$clazzName").receiver(jsValueClazzName)
    func.returns(ClassName(callbackInfo.packageName, getCallbackProxyName(callbackInfo)))
    func.addCode(
        """
        |return ${getCallbackProxyName(callbackInfo)}(this)
        |""".trimMargin()
    )
    return func.build()
}

fun genCallbackProxyClass(callbackInfo: CallbackInfo): TypeSpec {
    val proxyClassTypeSpec = TypeSpec.classBuilder(getCallbackProxyName(callbackInfo))
            .addSuperinterface(callbackInfo.clazzName.toTypeName())
    proxyClassTypeSpec.primaryConstructor(
        FunSpec.constructorBuilder().addParameter("_value", jsValueClazzName).build()
    )
    proxyClassTypeSpec.addProperty(PropertySpec.builder("_value", jsValueClazzName).initializer("_value").build())
    callbackInfo.functionList.forEach {
        proxyClassTypeSpec.addFunction(genProxyFunctionSpec(it, "_value.callMethod"))
    }
    return proxyClassTypeSpec.build()
}


fun parseCallbackList(resolver: Resolver): List<CallbackInfo> {
    val annotationName = KNCallback::class.qualifiedName!!
    val symbols = resolver.getSymbolsWithAnnotation(annotationName)
    val ksAnnotatedList: List<KSAnnotated> =
        symbols.filter { it is KSClassDeclaration && it.validate() && it.classKind == ClassKind.INTERFACE }.toList()

    val list = mutableListOf<CallbackInfo>()
    ksAnnotatedList.forEach {
        if (it !is KSClassDeclaration) {
            return@forEach
        }
        val packageName = it.packageName.asString()
        list.add(CallbackInfo(it.asStarProjectedType(), packageName, parseFunctionsList(it)))
    }
    return list
}

