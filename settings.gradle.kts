pluginManagement {
    val knActionMavenUrl: String? = System.getenv("KN_ACTION_BUILD_REPO_ABS")
        ?.let { java.io.File(it).toURI().toString() }
    repositories {
        knActionMavenUrl?.let { maven(it) }
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public")
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        val kotlinVersion: String by settings
        kotlin("multiplatform") version kotlinVersion
        val composeVersion: String by settings
        id("org.jetbrains.compose") version composeVersion
        kotlin("plugin.compose") version kotlinVersion
    }
}

dependencyResolutionManagement {
    val knActionMavenUrl: String? = System.getenv("KN_ACTION_BUILD_REPO_ABS")
        ?.let { java.io.File(it).toURI().toString() }
    repositories {
        knActionMavenUrl?.let { maven(it) }
        mavenLocal()
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public")
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        mavenCentral()
    }
}

rootProject.name = "compose-visibility-repro"

include("kotlinApp")
