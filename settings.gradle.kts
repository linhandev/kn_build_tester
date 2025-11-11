pluginManagement {
    repositories {
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        maven("/Volumes/disk/git/kmp/parallel-20/build/repo")
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        maven("/Volumes/disk/git/kmp/parallel-20/build/repo")
        mavenCentral()
    }
}

rootProject.name = "c2k"
