pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenLocal()
        apply(from = "${rootDir}/build_properties.gradle")
        maven(url = "https://artifact.bytedance.com/repository/releases")
        val buildProperties = extensions.findByName("build_properties") as Map<String, Any?>
        buildProperties["custom_maven_url"]?.let {
            maven(url = it)
        }
        mavenCentral()
        google()
    }
}

plugins {
    id("com.bytekmp.settings")
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        val buildProperties = extensions.findByName("build_properties") as Map<String, Any?>
        mavenLocal()
        maven(url = "https://artifact.bytedance.com/repository/releases")
        buildProperties["custom_maven_url"]?.let {
            maven(url = it)
        }
        mavenCentral()
    }
}

rootProject.name = "Bytekmp_sample"

// demo App
include(":app")
include(":app:ohosApp")
include(":app:androidApp")

include(":launcher")
include(":sample")
include(":todo")
