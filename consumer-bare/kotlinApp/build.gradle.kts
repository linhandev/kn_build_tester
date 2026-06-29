plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "22-0.1-SNAPSHOT"

fun String.capitalize() = replaceFirstChar { it.uppercase() }

kotlin {
    ohosArm64 {
        // HiLog bindings now come from the cpf 0.4-built klib published to maven local
        // (com.example:ohos-capi:22-0.1-SNAPSHOT, package platform.PerformanceAnalysisKit.HiLog),
        // built in the kn_samples-ohos-def repo. No local cinterop here.
        binaries {
            sharedLib {
                baseName = "c2k"
                freeCompilerArgs += "-Xadd-light-debug=enable"
                // Keep runtime/static libs' DWARF in the linked .so (pairs with kotlin.native.isNativeRuntimeDebugInfoEnabled in Kotlin repo local.properties).
                freeCompilerArgs += "-Xbinary=stripDebugInfoFromNativeLibs=false"
                // Set SONAME so libentry.so's NEEDED records bare "libc2k.so" (not the host
                // absolute IMPORTED_LOCATION path), letting the device dlopen it from the app's
                // libs dir. Without this, runHelloWorld fails with {} (libentry.so can't load).
                freeCompilerArgs += "-linker-options=-Wl,-soname,libc2k.so"
                // --as-needed: only NEEDED libs whose symbols are actually referenced (UND) by
                // libc2k.so. The klib aggregate carries linkerOpts for all 159 defs incl. HMS Kits
                // whose .so the consumer sysroot may lack; --as-needed drops the unreferenced ones.
                freeCompilerArgs += "-linker-options=-Wl,--as-needed"
                // HMS sysroot (.so stubs for HarmonyOS-SDK-Only Kits) checked into the repo.
                // The ohos-capi aggregate carries linkerOpts for all 159 defs incl. HMS Kits;
                // --as-needed drops unused ones from NEEDED but ld must locate them to judge
                // unreferenced, so add -L here. (After #14 split, non-HMS consumers won't need this.)
                val hmsLib = "${rootProject.projectDir.parentFile}/sysroot/sysroot-hms-aarch64-6.0.2.640-02/usr/lib/aarch64-linux-ohos"
                freeCompilerArgs += "-linker-options=-L$hmsLib"
            }
        }
    }
    sourceSets {
        val ohosArm64Main by getting {
            dependencies {
                // klib built with cpf 0.4 (abi_version 2.2.0); read by 2.3.20-HUAWEI (same major 2.x).
                implementation("com.example:ohos-capi:22-0.1-SNAPSHOT")
                // static-lib-demo: cinterop with staticLibraries (.a embedded in klib).
                // Consumer links the .a automatically (KGP handles included .a), no -L/-l needed.
                implementation("com.example:static-lib-demo:22-0.1-SNAPSHOT")
            }
        }
    }
}

arrayOf("debug", "release").forEach { type ->
    fun normalizeDir(dir: String) = dir.trim('/', '\\')
    // Copy after link in doLast: Gradle's Copy task was NO-SOURCE when from() pointed at files
    // that did not exist yet at configuration time, so libc2k.so never reached harmonyApp → crashes.
    val publishTask = tasks.register("publish${type.capitalize()}BinariesToHarmonyApp") {
        group = "harmony"
        val baseName: String by project
        val buildTaskSuffix: String by project
        val harmonyAppDir: String by project
        val soFileDst: String by project
        val hFileDst: String by project

        val buildTaskName = "link${type.capitalize()}Shared${buildTaskSuffix.capitalize()}"
        dependsOn(buildTaskName)

        doLast {
            val binDir = layout.buildDirectory.get().asFile.resolve("bin/ohosArm64/${type}Shared")
            val soSrc = binDir.resolve("lib${baseName}.so")
            val headerSrc = binDir.resolve("lib${baseName}_api.h")
            check(soSrc.exists()) { "Missing $soSrc after $buildTaskName" }
            check(headerSrc.exists()) { "Missing $headerSrc after $buildTaskName" }
            copy {
                into(rootProject.file(harmonyAppDir))
                from(soSrc) { into(normalizeDir(soFileDst)) }
                from(headerSrc) { into(normalizeDir(hFileDst)) }
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

tasks.findByName("linkDebugSharedOhosArm64")?.outputs?.upToDateWhen { false }
