rootProject.name = "KMPMultiplatform"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Local Maven repo at the repo root (kn_sample/m2, gitignored). Producer publishes klibs here;
// consumers resolve com.example:ohos-capi / static-lib-demo from here instead of ~/.m2.
// Inlined in each block: pluginManagement/dependencyResolutionManagement don't see top-level vals.
pluginManagement {
    val localProps = java.util.Properties().apply {
        runCatching { java.io.File("local.properties").inputStream() }.getOrNull()?.let { load(it) }
    }
    val huaweiUser = System.getenv("HUAWEI_MAVEN_USER") ?: localProps.getProperty("huaweiMavenUser")
    val huaweiPass = System.getenv("HUAWEI_MAVEN_PASS") ?: localProps.getProperty("huaweiMavenPass")
    val colabUser = System.getenv("COLAB_MAVEN_USER") ?: localProps.getProperty("colabMavenUser")
    val colabPass = System.getenv("COLAB_MAVEN_PASS") ?: localProps.getProperty("colabMavenPass")
    repositories {
        // colab: 发布 org.cpf.kotlin klib 的仓库(凭据在 local.properties)
        maven {
            url = uri("https://packages.aliyun.com/687e79a0e94e043d2d0f76ea/maven/colab")
            if (colabUser != null && colabPass != null) {
                credentials { username = colabUser; password = colabPass }
                authentication { create("basic", org.gradle.authentication.http.BasicAuthentication::class.java) }
            }
        }
        // 本地 m2:测试期走本地 m2(producer 发布的待测版本);发布期改回 colab 远程仓。
        // 开发流程:有变更测试时先走本地 m2,发布完新版本后再走 colab(见根 agent.md)。
        // 当前 22-0.2 已发 colab,走 colab 远程仓验证(本地 m2 注释)。
        // maven { url = uri(rootDir.parentFile.resolve("m2")) }
        maven {
            url = uri("https://devrepo.devcloud.cn-north-4.huaweicloud.com/artgalaxy/cn-north-4_a8338babc8534bb8aabb062c35845155_maven_7_1/")
            if (huaweiUser != null && huaweiPass != null) {
                credentials { username = huaweiUser; password = huaweiPass }
                authentication { create("basic", org.gradle.authentication.http.BasicAuthentication::class.java) }
            }
        }
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    val localProps = java.util.Properties().apply {
        runCatching { java.io.File("local.properties").inputStream() }.getOrNull()?.let { load(it) }
    }
    val huaweiUser = System.getenv("HUAWEI_MAVEN_USER") ?: localProps.getProperty("huaweiMavenUser")
    val huaweiPass = System.getenv("HUAWEI_MAVEN_PASS") ?: localProps.getProperty("huaweiMavenPass")
    val colabUser = System.getenv("COLAB_MAVEN_USER") ?: localProps.getProperty("colabMavenUser")
    val colabPass = System.getenv("COLAB_MAVEN_PASS") ?: localProps.getProperty("colabMavenPass")
    repositories {
        // colab: 发布 org.cpf.kotlin klib 的仓库(凭据在 local.properties)
        maven {
            url = uri("https://packages.aliyun.com/687e79a0e94e043d2d0f76ea/maven/colab")
            if (colabUser != null && colabPass != null) {
                credentials { username = colabUser; password = colabPass }
                authentication { create("basic", org.gradle.authentication.http.BasicAuthentication::class.java) }
            }
        }
        // 本地 m2:测试期走本地 m2(producer 发布的待测版本);发布期改回 colab 远程仓。
        // 开发流程:有变更测试时先走本地 m2,发布完新版本后再走 colab(见根 agent.md)。
        // 当前 22-0.2 已发 colab,走 colab 远程仓验证(本地 m2 注释)。
        // maven { url = uri(rootDir.parentFile.resolve("m2")) }
        maven {
            url = uri("https://devrepo.devcloud.cn-north-4.huaweicloud.com/artgalaxy/cn-north-4_a8338babc8534bb8aabb062c35845155_maven_7_1/")
            if (huaweiUser != null && huaweiPass != null) {
                credentials { username = huaweiUser; password = huaweiPass }
                authentication { create("basic", org.gradle.authentication.http.BasicAuthentication::class.java) }
            }
        }
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
