plugins {
    `kotlin-dsl`
}

repositories {
    mavenLocal()
    apply(from = "${rootDir}/../build_properties.gradle")
    val buildProperties = extensions.findByName("build_properties") as Map<String, Any?>
    mavenLocal()
    maven(url = "https://artifact.bytedance.com/repository/releases")
    buildProperties["custom_maven_url"]?.let {
        maven(url = it)
    }
    mavenCentral()
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        register("bytekmpSettings") {
            id = "com.bytekmp.settings"
            implementationClass = "com.bytekmp.buildlogic.ByteKmpSettingsPlugin"
        }
    }
}
