rootProject.name = "KMPMultiplatform"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    val localProps = java.util.Properties().apply {
        runCatching { java.io.File("local.properties").inputStream() }.getOrNull()?.let { load(it) }
    }
    val huaweiUser = System.getenv("HUAWEI_MAVEN_USER") ?: localProps.getProperty("huaweiMavenUser")
    val huaweiPass = System.getenv("HUAWEI_MAVEN_PASS") ?: localProps.getProperty("huaweiMavenPass")
    repositories {
        mavenLocal()
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
    repositories {
        mavenLocal()
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
