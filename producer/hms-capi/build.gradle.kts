import java.util.Properties

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "org.cpf.kotlin"

base.archivesName.set("hms-capi")

// HMS 扩展 Kit klib(AIP/CANN/DeviceSecurity/HandWrite/XEngine 等 19 个 def)。headers 在 HMS
// sysroot(cpf 0.4 的 additionalTargetSysRoot,DevEco 主 sysroot 不带),cinterop 不自动加 -I,
// 这里显式加。linkerOpts 的 -l 全是 HMS-only 库(24 个,见 wrap-linkeropts.py 勘察),与
// ohos-capi 的 144 def 零混合,拆分边界天然干净。

val defDir = file("$projectDir/nativeInterop/ohosArm64")
val cinteropOutDir = layout.buildDirectory.dir("classes/kotlin/ohosArm64/main/cinterop")

// HMS sysroot(headers + .so stub for HarmonyOS-SDK-Only Kits),check 进仓库 sysroot/。
val repoRoot = rootProject.projectDir.parentFile!!
val hmsInclude = file("$repoRoot/sysroot/sysroot-hms-aarch64-6.0.2.640-02/usr/include")

// ── 跨坐标 depends:hms def 的 depends 可能引用 ohos-capi 的 def(5 个:Ar_engine→
// OH_NativeBuffer, CANN→NeuralNetworkRuntime, GraphicsAccelerate→vulkan, Retrieval→RDB,
// XEngine→vulkan+gles3)。cinterop 要 -library 指向 ohos-capi 模块产的对应 klib,且传完整
// 传递闭包(cinterop -library 只加载直接 dep,传递依赖不自动解析)。所以要在 ohos-capi 的
// depends 图上算闭包。 ──
val ohosCapiProject = project(":ohos-capi")
val ohosCapiDefDir = ohosCapiProject.file("nativeInterop/ohosArm64")
data class DefInfo(val name: String, val depends: List<String>)

fun parseDefs(dir: java.io.File): List<DefInfo> =
    dir.listFiles { f -> f.extension == "def" }!!
        .map { f ->
            val name = f.nameWithoutExtension
            val deps = f.readLines()
                .firstOrNull { it.startsWith("depends") }
                ?.substringAfter("=")?.trim()?.split(Regex("\\s+"))?.filter { it.isNotBlank() }
                ?: emptyList()
            DefInfo(name, deps)
        }

val hmsDefs: List<DefInfo> = parseDefs(defDir)
val hmsDefByName = hmsDefs.associateBy { it.name }
val ohosCapiDefByName: Map<String, DefInfo> = parseDefs(ohosCapiDefDir).associateBy { it.name }

// 在给定 defByName 图上算传递闭包。
fun transitiveClosure(start: String, graph: Map<String, DefInfo>): List<String> {
    val seen = linkedSetOf<String>()
    fun visit(n: String) {
        graph[n]?.depends?.forEach { d ->
            if (graph.containsKey(d) && seen.add(d)) visit(d)
        }
    }
    visit(start)
    return seen.toList()
}

// hms 模块内闭包(同模块 dep,当前 19 个 hms def 无互相 depends,逻辑保留)。
fun hmsClosure(name: String) = transitiveClosure(name, hmsDefByName)
// 跨边界闭包:某跨边界 dep 在 ohos-capi 图上的完整闭包(含传递,如 XEngine→vulkan→? )。
fun ohosCapiClosure(dep: String) = transitiveClosure(dep, ohosCapiDefByName)

// 判断 dep 是否在 ohos-capi 侧(跨边界)。
fun isOhosCapiDep(dep: String) = ohosCapiDefByName.containsKey(dep)

kotlin {
    ohosArm64 {
        compilations {
            val main by getting {
                cinterops {
                    hmsDefs.forEach { d ->
                        create(d.name) {
                            defFile("$defDir/${d.name}.def")
                            extraOpts("-Xshort-module-name", d.name)
                            // depends 自闭环到 org.cpf.kotlin 坐标(同 ohos-capi,见 #18 根因)。
                            extraOpts("-no-default-libs")
                            // HMS sysroot 扩展 Kit 头(cinterop 不自动加 additionalTargetSysRoot)。
                            extraOpts("-compiler-option", "-I$hmsInclude")
                            // -library 加载 depends:同模块 dep 用本模块输出,跨边界 dep 用
                            // ohos-capi 模块输出(传完整闭包,cinterop -library 不自动解析传递)。
                            hmsClosure(d.name).forEach { dep ->
                                val depDir = cinteropOutDir.map { it.file("hms-capi-cinterop-${dep}").asFile.absolutePath }.get()
                                extraOpts("-library", depDir)
                            }
                            d.depends.filter { isOhosCapiDep(it) }.forEach { dep ->
                                ohosCapiClosure(dep).forEach { transDep ->
                                    val depDir = ohosCapiProject.layout.buildDirectory.dir("classes/kotlin/ohosArm64/main/cinterop")
                                        .map { it.file("ohos-capi-cinterop-${transDep}").asFile.absolutePath }.get()
                                    extraOpts("-library", depDir)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    sourceSets {
        val ohosArm64Main by getting {
            dependencies {
                // POM 传递 depend:consumer 加 hms-capi 坐标自动拉 ohos-capi(因 5 个 hms def
                // 跨坐标依赖 ohos def,consumer 必须同时有 ohos-capi 才能解析 manifest depends)。
                // cinterop 构建期 klib 解析靠上面的 -library,不靠此 project 依赖。
                implementation(project(":ohos-capi"))
            }
        }
    }
}

// Task ordering:跨边界 dep 的 cinterop task 在 ohos-capi 模块,hms 的 task 要等它们完成。
fun cinteropTaskName(name: String) = "cinterop${name.replaceFirstChar { it.uppercase() }}OhosArm64"
hmsDefs.forEach { d ->
    d.depends.filter { isOhosCapiDep(it) }.forEach { dep ->
        tasks.findByName(cinteropTaskName(d.name))?.dependsOn(":ohos-capi:${cinteropTaskName(dep)}")
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            artifactId = "hms-capi"
        }
    }
    repositories {
        maven { url = uri(rootProject.projectDir.parentFile.resolve("m2")) }
        val localProps = Properties().apply {
            runCatching { rootProject.file("local.properties").inputStream() }.getOrNull()?.let { load(it) }
        }
        val colabUser = System.getenv("COLAB_MAVEN_USER") ?: localProps.getProperty("colabMavenUser")
        val colabPass = System.getenv("COLAB_MAVEN_PASS") ?: localProps.getProperty("colabMavenPass")
        if (!colabUser.isNullOrEmpty() && !colabPass.isNullOrEmpty()) {
            maven {
                name = "colab"
                url = uri("https://packages.aliyun.com/687e79a0e94e043d2d0f76ea/maven/colab")
                credentials { username = colabUser; password = colabPass }
                authentication { create("basic", org.gradle.authentication.http.BasicAuthentication::class.java) }
            }
        }
    }
}

// colab 不允许同路径覆盖(409),只发 target publication,跳过 metadata(见 ohos-capi 同款注释)。
tasks.matching { it.name == "publishKotlinMultiplatformPublicationToColabRepository" }.configureEach { enabled = false }
