plugins {
    kotlin("multiplatform") version "2.0.21-KBA-014"
}

group = "com.example"
version = "1.0-SNAPSHOT"

fun String.capitalize() = replaceFirstChar { it.uppercase() }

kotlin {
    ohosArm64 {
        binaries {
            sharedLib {
                baseName = "c2k"
            }
        }
    }
}

arrayOf("debug", "release").forEach { type ->
    tasks.register<Copy>("startHarmonyApp${type.capitalize()}") {
        group = "harmony"
        dependsOn("link${type.capitalize()}SharedOhosArm64")

        notCompatibleWithConfigurationCache("Uses project.exec and file() at execution time")

        val harmonyAppDir: String by project
        val harmonyAppEntryModuleDir: String by project
        val hFileDir: String by project
        val soFileDir: String by project
        val absoluteHarmonyAppDir = if (File(harmonyAppDir).isAbsolute) harmonyAppDir else
            rootProject.file(harmonyAppDir).absolutePath

        fun normalizeDir(dir: String) = dir.trim('/', '\\')
        val entryDir = normalizeDir(harmonyAppEntryModuleDir)
        val headerSubDir = normalizeDir(hFileDir)
        val soSubDir = normalizeDir(soFileDir)
        val libBase = "c2k"
        val headerSrc = file("build/bin/ohosArm64/${type}Shared/lib${libBase}_api.h")
        val soSrc = file("build/bin/ohosArm64/${type}Shared/lib${libBase}.so")
        val headerDest = file("$absoluteHarmonyAppDir/$entryDir/$headerSubDir/lib${libBase}_api.h")
        val soDest = file("$absoluteHarmonyAppDir/$entryDir/$soSubDir/lib${libBase}.so")

        into(rootProject.file(absoluteHarmonyAppDir))
        from(headerSrc) { into("$entryDir/$headerSubDir") }
        from(soSrc) { into("$entryDir/$soSubDir") }

        inputs.files(headerSrc, soSrc)
        outputs.files(headerDest, soDest)

        doFirst {
            if (!headerSrc.exists() || !soSrc.exists()) {
                throw GradleException("Native artifacts not found. Expected $headerSrc and $soSrc. Did link${type.capitalize()}SharedOhosArm64 finish?")
            }
            if (!File(absoluteHarmonyAppDir).exists()) {
                throw GradleException("Harmony app dir does not exist: $absoluteHarmonyAppDir (check harmonyAppDir in gradle.properties)")
            }
        }

        doLast {
            val appJsonContent = file("$absoluteHarmonyAppDir/AppScope/app.json5").readText()
            val bundleNameRegex = """"bundleName"\s*:\s*"([^"]+)"""".toRegex()
            val bundleName = bundleNameRegex.find(appJsonContent)?.groupValues?.get(1)
                ?: error("bundleName not found")
            val devEcoStudioDir: String by project
            val abilityName: String by project
            val nodeHome = "$devEcoStudioDir/Contents/tools/node"
            val devEcoSdkHome = "$devEcoStudioDir/Contents/sdk"

            if (!File(devEcoStudioDir).exists()) {
                throw GradleException("Provided DevEco Studio install path doesn't exist: $devEcoStudioDir")
            }

            fun execAtHarmonyAppDir(cmd: List<String>) {
                val result = project.exec {
                    environment(mapOf("NODE_HOME" to nodeHome, "DEVECO_SDK_HOME" to devEcoSdkHome))
                    commandLine(cmd)
                    workingDir(absoluteHarmonyAppDir)
                    isIgnoreExitValue = true
                }
                if (result.exitValue != 0) {
                    throw GradleException("${cmd.joinToString(" ")}\nFailed with exit code ${result.exitValue}\nTry to reproduce this in DevEco Studio's command line, otherwise you would need to setup some enviroment parameters")
                }
            }

            println("=== Step 1: Install OHPM dependencies ===")
            execAtHarmonyAppDir(listOf(
                "$devEcoStudioDir/Contents/tools/ohpm/bin/ohpm",
                "install",
                "--all",
                "--registry",
                "https://ohpm.openharmony.cn/ohpm/",
                "--strict_ssl",
                "true"
            ))

            println("=== Step 2: Run hvigor sync ===")
            execAtHarmonyAppDir(listOf(
                "$devEcoStudioDir/Contents/tools/node/bin/node",
                "$devEcoStudioDir/Contents/tools/hvigor/bin/hvigorw.js",
                "--sync",
                "-p", "product=default",
                "-p", "buildMode=${type}",
                "--analyze=false",
                "--parallel",
                "--incremental",
                "--daemon"
            ))

            println("=== Step 3: Build HAP ===")
            execAtHarmonyAppDir(listOf(
                "$devEcoStudioDir/Contents/tools/node/bin/node",
                "$devEcoStudioDir/Contents/tools/hvigor/bin/hvigorw.js",
                "--mode", "module",
                "-p", "module=entry@default",
                "-p", "product=default",
                "-p", "buildMode=${type}",
                "-p", "requiredDeviceType=phone",
                "assembleHap",
                "--analyze=false",
                "--parallel",
                "--incremental",
                "--daemon"
            ))

            println("=== Step 4: Install HAP to device via hdc ===")
            val hapDir = File("$absoluteHarmonyAppDir/$entryDir/build/default/outputs/default")
            val signedHap = File(hapDir, "entry-default-signed.hap")
            val unsignedHap = File(hapDir, "entry-default-unsigned.hap")
            val hapFile = when {
                signedHap.exists() -> signedHap
                unsignedHap.exists() -> unsignedHap
                else -> throw GradleException("HAP file not found in ${hapDir.absolutePath}. Expected entry-default-signed.hap or entry-default-unsigned.hap")
            }

            execAtHarmonyAppDir(listOf(
                "$devEcoStudioDir/Contents/sdk/default/openharmony/toolchains/hdc",
                "install",
                hapFile.absolutePath
            ))

            println("=== Step 5: Launch app on device ===")
            println("Ability: $abilityName, Bundle: $bundleName")
            execAtHarmonyAppDir(listOf(
                "$devEcoStudioDir/Contents/sdk/default/openharmony/toolchains/hdc",
                "shell",
                "aa",
                "start",
                "-a", abilityName,
                "-b", bundleName
            ))
        }
    }
}
