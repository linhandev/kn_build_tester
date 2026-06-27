pluginManagement {
    repositories {
        mavenLocal()
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
        mavenLocal()
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public")
        mavenCentral()
    }
}

rootProject.name = "c2k"

include("hilog-klib")
