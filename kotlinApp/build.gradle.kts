plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0-SNAPSHOT"

fun String.capitalize() = replaceFirstChar { it.uppercase() }

kotlin {
    ohosArm64 {
        val ohosLinkerOpts = listOf("-lhilog_ndk.z")
        // KN sharedLibs UND `main` via __libc_start_main; on-demand dlopen needs a real def.
        val mainStubDir = rootProject.file("harmonyApp/entry/libs/arm64-v8a").absolutePath
        val mainStubOpts = listOf("-L$mainStubDir", "-lmainstub")
        // unique_name = com.example:k2n / com.example:n2k — must match moduleIncludes.
        val k2nDep = project(":k2n")
        val n2kDep = project(":n2k")
        val modularArgs = listOf(
            "-Xbinary=runtimeName=runtime",
            "-Xbinary=stdlibName=std",
            "-Xbinary=splitBCfile=8",
        )

        binaries.sharedLib("runtime") {
            baseName = "runtime"
            export(k2nDep)
            export(n2kDep)
            freeCompilerArgs += modularArgs + "-Xbinary=emitRuntime=true"
            linkerOpts(ohosLinkerOpts)
        }

        binaries.sharedLib("std") {
            baseName = "std"
            export(k2nDep)
            export(n2kDep)
            freeCompilerArgs += modularArgs + "-Xbinary=emitStdlib=true"
            linkerOpts(ohosLinkerOpts)
        }

        // Business SO #1: only its own klib in moduleIncludes (no cross-key dump).
        binaries.sharedLib("k2n") {
            baseName = "k2n"
            export(k2nDep)
            export(n2kDep)
            freeCompilerArgs += modularArgs + listOf(
                "-Xbinary=moduleIncludes={k2n:[com.example:k2n]}",
                "-Xbinary=outputModule=k2n",
            )
            linkerOpts(ohosLinkerOpts + mainStubOpts)
        }

        // Business SO #2: only its own klib — must NOT list k2n.
        binaries.sharedLib("n2k") {
            baseName = "n2k"
            export(k2nDep)
            export(n2kDep)
            freeCompilerArgs += modularArgs + listOf(
                "-Xbinary=moduleIncludes={n2k:[com.example:n2k]}",
                "-Xbinary=outputModule=n2k",
            )
            linkerOpts(ohosLinkerOpts + mainStubOpts)
        }
    }

    sourceSets {
        val ohosArm64Main by getting {
            dependencies {
                api(project(":k2n"))
                api(project(":n2k"))
            }
        }
    }
}

val modularLibs = listOf("runtime", "std", "k2n", "n2k")

arrayOf("debug", "release").forEach { type ->
    fun normalizeDir(dir: String) = dir.trim('/', '\\')

    val publishTask = tasks.register("publish${type.capitalize()}BinariesToHarmonyApp") {
        group = "harmony"
        val harmonyAppDir: String by project
        val soFileDst: String by project
        val hFileDst: String by project

        val linkTasks = modularLibs.map { lib ->
            "link${lib.capitalize()}${type.capitalize()}SharedOhosArm64"
        }
        dependsOn(linkTasks)

        doLast {
            for (lib in modularLibs) {
                val binDir = layout.buildDirectory.get().asFile
                    .resolve("bin/ohosArm64/${lib}${type.capitalize()}Shared")
                val soSrc = binDir.resolve("lib${lib}.so")
                val headerSrc = binDir.resolve("lib${lib}_api.h")
                check(soSrc.exists()) { "Missing $soSrc" }
                check(headerSrc.exists()) { "Missing $headerSrc" }
                copy {
                    into(rootProject.file(harmonyAppDir))
                    from(soSrc) { into(normalizeDir(soFileDst)) }
                    from(headerSrc) { into(normalizeDir(hFileDst)) }
                }
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
            val bundleNameRegex = """"bundleName"\s*:\s*"([^"]+)"""".toRegex()
            val bundleName = bundleNameRegex.find(appJsonContent)?.groupValues?.get(1) ?: error("bundleName not found")
            val devEcoStudioDir: String by project
            val abilityName: String by project
            val nodeHome = "$devEcoStudioDir/Contents/tools/node"
            val devEcoSdkHome = "$devEcoStudioDir/Contents/sdk"

            if (!File(devEcoStudioDir).exists()) {
                throw GradleException("Provided DevEco Studio install path doesn't exist: $devEcoStudioDir")
            }

            fun execAtHarmonyAppDir(cmd: List<String>) {
                val result = project.exec {
                    environment(
                        mapOf(
                            "NODE_HOME" to nodeHome,
                            "DEVECO_SDK_HOME" to devEcoSdkHome,
                            "PATH" to "$nodeHome/bin:${System.getenv("PATH")}"
                        )
                    )
                    commandLine(cmd)
                    workingDir(absoluteHarmonyAppDir)
                    isIgnoreExitValue = true
                }
                if (result.exitValue != 0) {
                    throw GradleException("${cmd.joinToString(" ")}\nFailed with exit code ${result.exitValue}\nTry to reproduce this in DevEco Studio's command line, otherwise you would need to setup some enviroment parameters")
                }
            }

            println("=== Step 1: Install OHPM dependencies ===")
            execAtHarmonyAppDir(
                listOf(
                    "$devEcoStudioDir/Contents/tools/ohpm/bin/ohpm",
                    "install",
                    "--all",
                    "--registry",
                    "https://ohpm.openharmony.cn/ohpm/",
                    "--strict_ssl",
                    "true"
                )
            )

            println("=== Step 2: Run hvigor sync ===")
            execAtHarmonyAppDir(
                listOf(
                    "$devEcoStudioDir/Contents/tools/node/bin/node",
                    "$devEcoStudioDir/Contents/tools/hvigor/bin/hvigorw.js",
                    "--sync",
                    "-p",
                    "product=default",
                    "-p",
                    "buildMode=${type}",
                    "--analyze=false",
                    "--parallel",
                    "--incremental"
                )
            )

            println("=== Step 3: Build HAP ===")
            execAtHarmonyAppDir(
                listOf(
                    "$devEcoStudioDir/Contents/tools/node/bin/node",
                    "$devEcoStudioDir/Contents/tools/hvigor/bin/hvigorw.js",
                    "--mode",
                    "module",
                    "-p",
                    "module=entry@default",
                    "-p",
                    "product=default",
                    "-p",
                    "buildMode=${type}",
                    "-p",
                    "requiredDeviceType=phone",
                    "assembleHap",
                    "--analyze=false",
                    "--parallel",
                    "--incremental"
                )
            )

            println("=== Step 4: Install HAP to device via hdc ===")
            val hapDir =
                File("$absoluteHarmonyAppDir/${normalizeDir(entryModuleDir)}/build/default/outputs/default")
            val signedHap = File(hapDir, "entry-default-signed.hap")
            val unsignedHap = File(hapDir, "entry-default-unsigned.hap")
            val hapFile = when {
                signedHap.exists() -> signedHap
                unsignedHap.exists() -> unsignedHap
                else -> throw GradleException("HAP file not found in ${hapDir.absolutePath}. Expected entry-default-signed.hap or entry-default-unsigned.hap")
            }

            execAtHarmonyAppDir(
                listOf(
                    "$devEcoStudioDir/Contents/sdk/default/openharmony/toolchains/hdc", "install", hapFile.absolutePath
                )
            )

            println("=== Step 5: Launch app on device ===")
            println("Ability: $abilityName, Bundle: $bundleName")
            execAtHarmonyAppDir(
                listOf(
                    "$devEcoStudioDir/Contents/sdk/default/openharmony/toolchains/hdc",
                    "shell",
                    "aa",
                    "start",
                    "-a",
                    abilityName,
                    "-b",
                    bundleName
                )
            )
        }
    }
}

modularLibs.forEach { lib ->
    tasks.findByName("link${lib.capitalize()}DebugSharedOhosArm64")?.outputs?.upToDateWhen { false }
}
