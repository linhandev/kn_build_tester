pluginManagement {
    repositories {
        maven("https://kmp-ohos-test.obs.cn-south-1.myhuaweicloud.com/maven")
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven(rootDir.resolve("maven-repo").toURI())
        maven("https://kmp-ohos-test.obs.cn-south-1.myhuaweicloud.com/maven")
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "c2k"

include("add")
