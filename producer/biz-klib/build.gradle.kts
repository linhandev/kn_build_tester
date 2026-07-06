import java.util.Properties

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

// 业务 klib:用 cpf 0.4 编译,代码里 import platform.PerformanceAnalysisKit.HiLog.*。
// 关键:不加 -no-default-libs,让 import 解析到 cpf 0.4 dist 的内置 platformLib
// (org.jetbrains.kotlin.native.platform.HiLog),于是本 klib 的 manifest depends 写
// org.jetbrains.kotlin.native.platform.HiLog —— 这正是消费者侧 patch 的目标:
// HUAWEI dist 没有这个库,patch 把 depends 重定向到 com.example:ohos-capi-cinterop-HiLog。
// 详见 task/独立capi封装/场景1-patch实现对比.html。

group = "org.cpf.kotlin"

base.archivesName.set("biz-klib")

kotlin {
    ohosArm64 {
        // 只发 klib,不出 .so。
    }
    sourceSets {
        val ohosArm64Main by getting {
            dependencies {
                // 不依赖 com.example:ohos-capi —— 业务 klib 故意让 HiLog 走 dist 的
                // org.jetbrains 坐标,而非 com.example 自闭环坐标,这样 depends 才写 org.jetbrains。
            }
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            artifactId = "biz-klib"
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
