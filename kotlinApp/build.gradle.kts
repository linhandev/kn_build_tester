plugins {
    kotlin("multiplatform")
    id("org.cpf.kotlin.akinterop-gradle-plugin") version "0.5.0-09"
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
    mavenLocal()
    google()
    mavenCentral()
}

fun String.capitalize() = replaceFirstChar { it.uppercase() }

kotlin {
    ohosArm64 {
        binaries {
            sharedLib {
                baseName = "kn"
                freeCompilerArgs += "-Xadd-light-debug=enable"
                (findProperty("cInterfaceMode") as? String)?.let {
                    freeCompilerArgs += "-Xbinary=cInterfaceMode=$it"
                }
                (findProperty("enableStackmap") as? String)?.let {
                    freeCompilerArgs += "-Xbinary=enableStackmap=$it"
                }
            }
        }
        compilations.forEach {
            it.compileTaskProvider.configure {
                compilerOptions {
                    optIn.addAll(
                        "kotlinx.cinterop.ExperimentalForeignApi",
                        "kotlin.experimental.ExperimentalNativeApi",
                    )
                }
            }
        }
    }
    sourceSets {
        val ohosArm64Main by getting { kotlin.srcDirs("src/ohosArm64Main/kotlin") }
    }
}

ffi {
    dtsOutput.set(rootProject.projectDir.resolve("harmonyApp/entry/src/main/cpp/types/libentry/ffi_export.d.ts"))
    debugSoOutputDir.set(rootProject.projectDir.resolve("harmonyApp/entry/libs/arm64-8a"))
}

arrayOf("debug", "release").forEach { type ->
    fun normalizeDir(dir: String) = dir.trim('/', '\\')
    val publishTask = tasks.register("publish${type.capitalize()}BinariesToHarmonyApp") {
        group = "harmony"
        val baseName: String by project
        val buildTaskSuffix: String by project
        val harmonyAppDir: String by project
        val soFileDst: String by project
        val buildTaskName = "link${type.capitalize()}Shared${buildTaskSuffix.capitalize()}"
        dependsOn(buildTaskName)
        doLast {
            val binDir = layout.buildDirectory.get().asFile.resolve("bin/ohosArm64/${type}Shared")
            val soSrc = binDir.resolve("lib${baseName}.so")
            check(soSrc.exists()) { "Missing $soSrc after $buildTaskName" }
            copy {
                into(rootProject.file(harmonyAppDir))
                from(soSrc) { into(normalizeDir(soFileDst)) }
            }
        }
    }
    tasks.register("startHarmonyApp${type.capitalize()}") {
        group = "harmony"
        dependsOn(publishTask)
        notCompatibleWithConfigurationCache("Uses project.exec and file() at execution time")
        outputs.upToDateWhen { false }
        val harmonyAppDir: String by project
        val entryModuleDir: String by project
        val absoluteHarmonyAppDir =
            if (File(harmonyAppDir).isAbsolute) harmonyAppDir else rootProject.file(harmonyAppDir).absolutePath
        doLast {
            val appJsonContent = file("$absoluteHarmonyAppDir/AppScope/app.json5").readText()
            val bundleNameRegex = """"bundleName"\s*:\s*"([^"]+)""""".toRegex()
            val bundleName = bundleNameRegex.find(appJsonContent)?.groupValues?.get(1) ?: error("bundleName not found")
            val devEcoStudioDir: String by project
            val abilityName: String by project
            val nodeHome = "$devEcoStudioDir/Contents/tools/node"
            val devEcoSdkHome = "$devEcoStudioDir/Contents/sdk"
            if (!File(devEcoStudioDir).exists()) throw GradleException("DevEco not found: $devEcoStudioDir")
            fun execAtHarmonyAppDir(cmd: List<String>) {
                val result = project.exec {
                    environment(mapOf("NODE_HOME" to nodeHome, "DEVECO_SDK_HOME" to devEcoSdkHome, "PATH" to "$nodeHome/bin:${System.getenv("PATH")}"))
                    commandLine(cmd); workingDir(absoluteHarmonyAppDir); isIgnoreExitValue = true
                }
                if (result.exitValue != 0) throw GradleException("${cmd.joinToString(" ")} failed: ${result.exitValue}")
            }
            execAtHarmonyAppDir(listOf("$devEcoStudioDir/Contents/tools/ohpm/bin/ohpm", "install", "--all", "--registry", "https://ohpm.openharmony.cn/ohpm/", "--strict_ssl", "true"))
            execAtHarmonyAppDir(listOf("$devEcoStudioDir/Contents/tools/node/bin/node", "$devEcoStudioDir/Contents/tools/hvigor/bin/hvigorw.js", "--sync", "-p", "product=default", "-p", "buildMode=${type}", "--analyze=false", "--parallel", "--incremental"))
            execAtHarmonyAppDir(listOf("$devEcoStudioDir/Contents/tools/node/bin/node", "$devEcoStudioDir/Contents/tools/hvigor/bin/hvigorw.js", "--mode", "module", "-p", "module=entry@default", "-p", "product=default", "-p", "buildMode=${type}", "-p", "requiredDeviceType=phone", "assembleHap", "--analyze=false", "--parallel", "--incremental"))
            val hapDir = File("$absoluteHarmonyAppDir/${normalizeDir(entryModuleDir)}/build/default/outputs/default")
            val signedHap = File(hapDir, "entry-default-signed.hap")
            val unsignedHap = File(hapDir, "entry-default-unsigned.hap")
            val hapFile = when { signedHap.exists() -> signedHap; unsignedHap.exists() -> unsignedHap; else -> throw GradleException("HAP not found in ${hapDir.absolutePath}") }
            execAtHarmonyAppDir(listOf("$devEcoStudioDir/Contents/sdk/default/openharmony/toolchains/hdc", "install", hapFile.absolutePath))
            execAtHarmonyAppDir(listOf("$devEcoStudioDir/Contents/sdk/default/openharmony/toolchains/hdc", "shell", "aa", "start", "-a", abilityName, "-b", bundleName))
        }
    }
}
tasks.findByName("linkDebugSharedOhosArm64")?.outputs?.upToDateWhen { false }
