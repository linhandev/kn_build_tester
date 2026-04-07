pluginManagement {
    repositories {
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        mavenCentral()
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
    }
}

rootProject.name = "c2k"

include("kotlinApp")
