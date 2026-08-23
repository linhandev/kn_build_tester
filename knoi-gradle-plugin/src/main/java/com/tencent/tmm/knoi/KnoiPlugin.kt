package com.tencent.tmm.knoi

import com.tencent.tmm.knoi.inject.addAndroidKSP
import com.tencent.tmm.knoi.inject.getKNOIConfigPath
import com.tencent.tmm.knoi.inject.injectAndroidConfig
import com.tencent.tmm.knoi.inject.injectKNOIConfig
import com.tencent.tmm.knoi.task.BinariesPublishForbiddenTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.external.project
import java.io.File

const val OPTION_MODULE_NAME = "MODULE_NAME"
const val OPTION_CONFIG_FILE = "CONFIG_FILE"
const val OPTION_IS_ANDROID_APP = "IS_ANDROID_APP"
const val OPTION_TYPESCRIPT_GEN_DIR = "TS_GEN_DIR"
const val OPTION_IS_BINARIES_MODULE = "IS_BINARIES_MODULE"
const val OPTION_IGNORE_TYPE_ASSERT = "IGNORE_TYPE_ASSERT"

abstract class KnoiPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("knoi", KnoiExtension::class.java)
        applyAndroid(project, extension)

        val kmp: KotlinMultiplatformExtension? =
            project.extensions.getByName("kotlin") as? KotlinMultiplatformExtension
        if (kmp != null) {
            injectKNOIConfig(project, kmp, extension)
            project.afterEvaluate {
                writeKSPArgs(project, kmp, extension)
                registerTask(project, kmp)
            }
        }
    }

    private fun applyAndroid(
        project: Project,
        extension: KnoiExtension
    ) {
        project.plugins.withId("com.android.application") {
            println("KNOIAndroid injectAndroidConfig")
            addAndroidKSP(project, extension)
            project.afterEvaluate {
                injectAndroidConfig(project, extension)
            }
        }
    }

}

fun writeKSPArgs(project: Project, kmp: KotlinMultiplatformExtension, extension: KnoiExtension) {
    val tsGenDir =
        extension.tsGenDir.ifBlank { project.buildDir.absolutePath + "/ts-api/" }
    val configStr = StringBuilder()
    configStr.append("${OPTION_TYPESCRIPT_GEN_DIR}=${tsGenDir}\n")
    configStr.append("${OPTION_IGNORE_TYPE_ASSERT}=${extension.ignoreTypeAssert}\n")
    configStr.append("${OPTION_IS_BINARIES_MODULE}=${isBinariesModule(kmp)}\n")
    val formatModuleName =
        extension.moduleName.ifBlank { project.name }.replace(Regex("[^A-Za-z]"), "")
    configStr.append("${OPTION_MODULE_NAME}=${formatModuleName}\n")
    val file = File(getKNOIConfigPath(project))
    file.delete()
    file.ensureParentDirsCreated()
    file.createNewFile()
    file.writeText(configStr.toString())
}

fun registerTask(project: Project, kmp: KotlinMultiplatformExtension) {
    if (isBinariesModule(kmp)) {
        try {
            val generateMetadataTask =
                project.tasks.getByName("generateMetadataFileForOhosArm64Publication")
            val forbiddenTask = project.tasks.register(
                "knoiBinariesPublishForbiddenTask", BinariesPublishForbiddenTask::class.java
            )
            generateMetadataTask.dependsOn(forbiddenTask)
        } catch (e: Exception) {
            // ignore it
        }
    }
}

@OptIn(ExternalKotlinTargetApi::class)
fun isBinariesModule(kmpExtension: KotlinMultiplatformExtension): Boolean {
    return try {
        kmpExtension.ohosArm64().binaries.size > 1
    } catch (error: Error) {
        // 注：Android 目前需要手动配置，后续做自动注入 KSP
        return kmpExtension.project.plugins.hasPlugin(KotlinCocoapodsPlugin::class.java)
    }
}
