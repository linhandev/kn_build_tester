plugins {
    `kotlin-dsl`
}
repositories {
    maven { url = uri(rootProject.projectDir.parentFile.resolve("m2")) }
    maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public")
    gradlePluginPortal()
    mavenCentral()
}
