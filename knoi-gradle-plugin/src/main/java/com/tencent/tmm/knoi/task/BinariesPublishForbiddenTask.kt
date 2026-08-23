package com.tencent.tmm.knoi.task

import com.tencent.tmm.knoi.isBinariesModule
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 *  禁止发布二进制模块，会导致 KNOI 生成的初始化类冲突
 */
open class BinariesPublishForbiddenTask : DefaultTask() {
    init {
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun check() {
        val kmp: KotlinMultiplatformExtension =
            project.extensions.getByName("kotlin") as KotlinMultiplatformExtension
        if (!isBinariesModule(kmp)) {
            return
        }
        throw BinariesPublishForbiddenException()
    }
}

class BinariesPublishForbiddenException :
    RuntimeException("Knoi Forbidden publish shared Binaries module, please check it.")