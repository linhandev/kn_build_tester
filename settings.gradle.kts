pluginManagement {
    repositories {
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
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "c2k"

include("add")
