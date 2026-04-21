import java.io.File

/** CI sets KN_ACTION_BUILD_REPO_ABS to an absolute path to kotlin build/repo (Maven layout). Optional for local dev. */
private fun knActionMavenUrl(): String? {
    val p = System.getenv("KN_ACTION_BUILD_REPO_ABS") ?: return null
    return File(p).toURI().toString()
}

pluginManagement {
    repositories {
        knActionMavenUrl()?.let { maven(it) }
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public")
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        knActionMavenUrl()?.let { maven(it) }
        mavenLocal()
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public")
        maven("https://mirrors.tencent.com/nexus/repository/maven-tencent")
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        mavenCentral()
    }
}

rootProject.name = "c2k"

include("kotlinApp")
