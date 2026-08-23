package com.tencent.tmm.knoi.inject

import com.google.devtools.ksp.gradle.KspExtension
import com.tencent.tmm.knoi.KnoiExtension
import com.tencent.tmm.knoi.OPTION_IGNORE_TYPE_ASSERT
import com.tencent.tmm.knoi.OPTION_IS_ANDROID_APP
import com.tencent.tmm.knoi.OPTION_IS_BINARIES_MODULE
import com.tencent.tmm.knoi.OPTION_MODULE_NAME
import com.tencent.tmm.knoi.OPTION_TYPESCRIPT_GEN_DIR
import org.gradle.api.Project

fun injectAndroidConfig(
    project: Project, extension: KnoiExtension
) {
    addKNOIDependencies(project, extension)
    configAndroidKsp(project, extension)
}

fun addAndroidKSP(
    project: Project, extension: KnoiExtension
) {

    project.pluginManager.apply("com.google.devtools.ksp")
    project.dependencies.add("ksp", getKNOIProcessorDependencies(extension, project))

}

private fun configAndroidKsp(project: Project, extension: KnoiExtension) {
    val ksp = project.extensions.getByName("ksp") as KspExtension

    ksp.arg(OPTION_IS_ANDROID_APP, true.toString())
    ksp.arg(OPTION_IGNORE_TYPE_ASSERT, extension.ignoreTypeAssert.toString())
    ksp.arg(OPTION_IS_BINARIES_MODULE, true.toString())
    val formatModuleName =
        extension.moduleName.ifBlank { project.name }.replace(Regex("[^A-Za-z]"), "")
    ksp.arg(OPTION_MODULE_NAME, formatModuleName)
}

private fun addKNOIDependencies(
    project: Project,
    extension: KnoiExtension
) {
    project.dependencies.add("implementation", getKNOIDependencies(extension, project))
    project.dependencies.add("implementation", getKNOIAnnotationDependencies(extension, project))
}