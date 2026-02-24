pluginManagement {
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public")
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven(rootDir.resolve("maven-repo").toURI())
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public")
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "c2k"

include("add")
