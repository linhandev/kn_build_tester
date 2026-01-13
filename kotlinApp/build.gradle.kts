plugins {
    kotlin("multiplatform") version "2.0.21-KBA-014"
}

group = "com.example"
version = "1.0-SNAPSHOT"

fun String.capitalize() = replaceFirstChar { it.uppercase() }

kotlin {
    ohosArm64 {
        compilations.getByName("main") {
            cinterops {
                val gcov by creating {
                    includeDirs(rootProject.file("harmonyApp/entry/src/main/cpp/include"))
                }
            }
            dependencies {
                implementation("com.example:switchLib:1.0-SNAPSHOT")
            }
        }
        binaries {
            sharedLib {
                baseName = "c2k"
                freeCompilerArgs += "-Xadd-light-debug=enable"
                freeCompilerArgs += listOf("-Xbinary=coverage=true", "-Xtemporary-files-dir=/tmp/inspect")
            }
        }
    }
}

arrayOf("debug", "release").forEach { type ->
    fun normalizeDir(dir: String) = dir.trim('/', '\\')
    val publishTask = tasks.register<Copy>("publish${type.capitalize()}BinariesToHarmonyApp") {
        group = "harmony"
        val baseName: String by project
        val buildTaskSuffix: String by project
        val harmonyAppDir: String by project
        val soFileDst: String by project
        val hFileDst: String by project

        val buildTaskName = "link${type.capitalize()}Shared${buildTaskSuffix.capitalize()}"
        dependsOn(buildTaskName)

        val binDir = tasks.getByName(buildTaskName).outputs.files.single()
        val soSrc = binDir.resolve("lib${baseName}.so")
        val headerSrc = binDir.resolve("lib${baseName}_api.h")

        into(rootProject.file(harmonyAppDir))
        from(headerSrc) { into(hFileDst) }
        from(soSrc) { into(soFileDst) }

        inputs.files(binDir)
        outputs.upToDateWhen { false }
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
                    "--incremental",
                    "--daemon"
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
                    "--incremental",
                    "--daemon"
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