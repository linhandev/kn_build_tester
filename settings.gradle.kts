pluginManagement {
    repositories {
        maven("/Volumes/disk/git/kmp/parallel-20/build/repo")
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("/Volumes/disk/git/kmp/parallel-20/build/repo")
        mavenCentral()
    }
}

rootProject.name = "c2k"
