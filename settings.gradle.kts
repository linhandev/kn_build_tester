pluginManagement {
    repositories {
        maven("https://kmp-ohos-test.obs.cn-south-1.myhuaweicloud.com/maven")
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://kmp-ohos-test.obs.cn-south-1.myhuaweicloud.com/maven")
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        mavenCentral()
    }
}

rootProject.name = "c2k"

include("multiply")
