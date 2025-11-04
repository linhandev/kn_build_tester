pluginManagement {
    repositories {
        maven("/Volumes/disk/git/kmp/20/build/repo")
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("/Volumes/disk/git/kmp/20/build/repo")
        mavenCentral()
    }
}

rootProject.name = "c2k"

include(":bizA", ":bizB")
