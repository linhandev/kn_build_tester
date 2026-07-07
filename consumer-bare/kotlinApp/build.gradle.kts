plugins {
    kotlin("multiplatform")
}

group = "org.cpf.kotlin"

fun String.capitalize() = replaceFirstChar { it.uppercase() }

// ─── patch depends 闭环(透明 ArtifactTransform,不侵入开发者依赖声明) ──────────
// 开发者写正常 maven 坐标 implementation("org.cpf.kotlin:biz-klib:..."),Gradle 解析出原始
// klib artifact(org.jetbrains.kotlin.klib type)后,buildSrc 里的 PatchBizKlibTransform
// 自动把它换成 patched 版本:manifest depends 从 org.jetbrains.kotlin.native.platform.HiLog
// 重定向到 org.cpf.kotlin:ohos-capi-cinterop-HiLog(m2 里 ohos-capi 聚合自带)。开发者无需感知。
// 盘上 patch:zip 解包 → 改 default/manifest → 重打包。无完整性校验。
// 详见 task/独立capi封装/场景1-patch实现对比.html。

// 重定向规则:org.jetbrains.kotlin.native.platform.AIP -> org.cpf.kotlin:ohos-capi-cinterop-AIP
// AIP 是 CPF 独有库(DataAugmentationKit),HUAWEI dist 既无此 unique_name 也无此 package 聚合,
// 是真"patch 必要"场景。
val bizDependRedirects = mapOf(
    "org.jetbrains.kotlin.native.platform.AIP" to "org.cpf.kotlin:ohos-capi-cinterop-AIP",
)

// 注册 transform(放 buildSrc 是因为 build script 顶层 registerTransform 的 lambda 类型
// 在 Kotlin DSL 里有歧义重载,SAM 转换不工作)。
// 当前验证:同 type transform 不触发,且按 FQN 解析可能不需要 patch。先注释掉,验证未 patch
// 时 bizAip() 调用能否 link 成功(消费者有 ohos-capi 提供 AIP package)。
// org.cpf.kotlin.patch.applyBizKlibPatchTransform(project, bizDependRedirects, namePrefix = "biz-klib")

kotlin {
    ohosArm64 {
        // HiLog bindings now come from the cpf 0.4-built klib published to maven local
        // (org.cpf.kotlin:ohos-capi:22-0.1-SNAPSHOT, package platform.PerformanceAnalysisKit.HiLog),
        // built in the kn_samples-ohos-def repo. No local cinterop here.
        binaries {
            sharedLib {
                baseName = "c2k"
                // Set SONAME so libentry.so's NEEDED records bare "libc2k.so" (not the host
                // absolute IMPORTED_LOCATION path), letting the device dlopen it from the app's
                // libs dir. Without this, runHelloWorld fails with {} (libentry.so can't load).
                freeCompilerArgs += "-linker-options=-Wl,-soname,libc2k.so"
                // 按需链接已下沉到每个 def 的 linkerOpts(--push-state --as-needed <libs> --pop-state),
                // 不再在这里加全局 -Wl,--as-needed。全局 --as-needed 会泄漏到 consumer 后续所有 -l
                // 和 trailing flags,改变它们的链接行为;def 内 push/pop 把 as-needed 限定在各 def
                // 的库组内,pop 后全局状态恢复 push 前,即"原来是 as 还是 as,原来是 noas 还是 noas"。
                //
                // 拆分后 ohos-capi(144 ohos def)不再带 hms 扩展 def 的 linkerOpts,bare 只调
                // AVTranscoder(ohos 侧),无需 HMS sysroot 的 -L(拆分收益:非 hms consumer 不配 HMS)。
                // 探针:故意 link 一个 bare 完全没用到的库(libEGL,ohos 主 sysroot 有 stub,不在任何
                // def 的 linkerOpts 里,KN 自动加 -L 指向 ohos 主 sysroot)。全局处于 no-as-needed
                // (lld 默认,且我们已去掉全局 --as-needed)时,这个未引用的探针库应进入 NEEDED;若 def
                // 的 push/pop 泄漏了 --as-needed 到全局,探针库会被当未引用丢弃,不在 NEEDED → 测试失败。
                // 见 scripts/check-asneeded-state.sh。
                freeCompilerArgs += "-linker-options=-lEGL"
            }
        }
    }
    sourceSets {
        val ohosArm64Main by getting {
            dependencies {
                // 隔离验证:只依赖 biz-klib(它 depends 写 org.jetbrains.kotlin.native.platform.AIP,
                // HUAWEI dist 无此库)。切换下面一行注释来对照"有/无独立 binding"两种情况。
                implementation("org.cpf.kotlin:biz-klib:${property("klibVersion")}")
                // ✅ 有独立 binding:解开下面这行,bizAip() 能 link(ohos-capi 提供 AIP package)
                implementation("org.cpf.kotlin:ohos-capi:${property("klibVersion")}")
                // ❌ 无独立 binding:注释掉上面这行,bizAip() 符号找不到,link 失败
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
