pluginManagement {
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public")
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        gradlePluginPortal()
        mavenCentral()
        maven("https://kmp-ohos-test.obs.cn-south-1.myhuaweicloud.com/maven")
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public")
        mavenLocal()
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        mavenCentral()
        maven("https://kmp-ohos-test.obs.cn-south-1.myhuaweicloud.com/maven")
    }
}

rootProject.name = "c2k"

include("kotlinApp")
include("switchLib")
include("sugarLib")
