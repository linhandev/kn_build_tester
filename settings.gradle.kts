rootProject.name = "metadata-klib-demo"

include(":lib")
include(":app")

pluginManagement {
    repositories {
        mavenLocal()
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositories {
        maven(rootProject.projectDir.resolve("repo").toURI())
        mavenLocal()
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent/")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
    }
}
