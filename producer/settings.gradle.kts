// Local Maven repo at the repo root (kn_sample/m2), gitignored. Producer publishes here,
// consumers resolve from here — keeps published klibs inspectable without touching ~/.m2.
// Defined inline in each block because pluginManagement/dependencyResolutionManagement don't see
// top-level settings vals.
pluginManagement {
    repositories {
        maven { url = uri(rootDir.parentFile.resolve("m2")) }
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public")
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        val kotlinVersion: String by settings
        kotlin("multiplatform") version kotlinVersion
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri(rootDir.parentFile.resolve("m2")) }
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public")
        mavenCentral()
    }
}

rootProject.name = "c2k"

include("ohos-capi")
include("static-lib-demo")
