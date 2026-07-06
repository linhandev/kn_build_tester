package org.cpf.kotlin.patch

import org.gradle.api.artifacts.transform.CacheableTransform
import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.artifacts.transform.TransformSpec
import org.gradle.api.Action
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 把 klib 的 manifest depends 重定向。透明 ArtifactTransform:开发者写正常 maven 坐标依赖,
 * Gradle 解析出原始 klib artifact 后,本 transform 自动换成 patched 版本。
 *
 * 仅对文件名以 [namePrefix] 开头的 klib 生效(默认 "biz-klib"),其它 klib 原样放过。
 * 盘上 patch:zip 解包 → 改 default/manifest 的 depends 行 → 重打包。无完整性校验。
 * 详见 task/独立capi封装/场景1-patch实现对比.html。
 */
@CacheableTransform
abstract class PatchBizKlibTransform : TransformAction<PatchBizKlibTransform.Parameters> {
    interface Parameters : TransformParameters {
        @get:Input
        val redirects: MapProperty<String, String>

        @get:Input
        val namePrefix: org.gradle.api.provider.Property<String>
    }

    @get:Classpath
    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val src = inputArtifact.get().asFile
        val prefix = parameters.namePrefix.get()
        if (!src.name.startsWith(prefix)) return  // 只 patch 目标 klib,其它原样放过
        val redirects = parameters.redirects.get()
        val manifest = readKlibManifest(src)
        if (redirects.keys.none { manifest.contains(it) }) return  // 无重定向 key,放过
        val dst = outputs.file("patched-${src.name}")
        patchKlibManifest(src, dst, redirects)
        org.gradle.api.logging.Logging.getLogger("PatchBizKlib").lifecycle("PATCHED biz-klib -> $dst")
    }
}

internal fun readKlibManifest(klib: java.io.File): String {
    ZipFile(klib).use { zf ->
        zf.getEntry("default/manifest").let { e -> return zf.getInputStream(e).bufferedReader().readText() }
    }
}

internal fun patchKlibManifest(src: java.io.File, dst: java.io.File, redirects: Map<String, String>) {
    ZipFile(src).use { zf ->
        ZipOutputStream(dst.outputStream()).use { zos ->
            for (e in zf.entries().toList()) {
                zos.putNextEntry(ZipEntry(e.name))
                if (e.name == "default/manifest") {
                    val text = zf.getInputStream(e).bufferedReader().readText()
                    zos.write(patchDependsLine(text, redirects).toByteArray())
                } else {
                    zf.getInputStream(e).use { it.copyTo(zos) }
                }
                zos.closeEntry()
            }
        }
    }
}

internal fun patchDependsLine(manifest: String, redirects: Map<String, String>): String {
    val lines = manifest.lines().toMutableList()
    for (i in lines.indices) {
        if (lines[i].startsWith("depends")) {
            val key = lines[i].substringAfter("=").trim()
            val deps = key.split(Regex("\\s+")).filter { it.isNotBlank() }
            lines[i] = "depends=" + deps.joinToString(" ") { redirects[it] ?: it }
        }
    }
    return lines.joinToString("\n")
}

/**
 * 在项目上注册 KLIB -> KLIB 的 artifact transform,把指定坐标的 klib manifest depends
 * 重定向。build script 顶层调用此函数即可,无需在 build script 里写有歧义的
 * registerTransform lambda。
 */
fun applyBizKlibPatchTransform(
    project: org.gradle.api.Project,
    redirects: Map<String, String>,
    namePrefix: String = "biz-klib",
) {
    project.dependencies.registerTransform(PatchBizKlibTransform::class.java) {
        from.attribute(
            org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
            "org.jetbrains.kotlin.klib",
        )
        to.attribute(
            org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
            "org.jetbrains.kotlin.klib",
        )
        parameters.redirects.set(redirects)
        parameters.namePrefix.set(namePrefix)
    }
}
