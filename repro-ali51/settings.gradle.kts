pluginManagement {
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        mavenCentral()
        google()
    }
}

rootProject.name = "repro-ali51"
include(":shared")
