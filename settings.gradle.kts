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

rootProject.name = "cexport-demo"

// Scenario 1: Main app module (top-level subproject that produces the binary)
include(":app")

// Scenario 2: Dependency subproject (submodule)
include(":lib-subproject")

// Scenario 3: Klib source (will be built as klib and depended on)
include(":lib-klib")
