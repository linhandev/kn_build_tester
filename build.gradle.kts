@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.library).apply(false)
    alias(libs.plugins.android.application).apply(false)
    id("org.jetbrains.kotlin.android").apply(false)
    id("org.jetbrains.kotlin.multiplatform").apply(false)
    id("org.jetbrains.kotlin.jvm").apply(false)
    id("org.jetbrains.kotlin.plugin.parcelize").apply(false)
    id("org.jetbrains.kotlin.plugin.serialization").apply(false)
    id("org.jetbrains.kotlin.plugin.compose").apply(false)
    id("org.jetbrains.kotlin.native.cocoapods").apply(false)
    id("com.google.devtools.ksp").apply(false)
    alias(libs.plugins.bytekmp.all).apply(false)
    alias(libs.plugins.jetbrains.compose).apply(false)
    alias(libs.plugins.bytekmp.leakcanary.plugin).apply(false)
}

buildscript {
    dependencies {
        classpath(libs.plugins.so.har.generator.get().toString())
        classpath(libs.plugins.compose.runtime.get().toString())
        classpath("com.bytedance.kmp.kmpleakcanary:kcp-plugin:${libs.versions.bytekmp.leakcanary.plugin}")
    }
}

// 忽略kotlin版本升级到2.0之后UnknownMetadataVersion大量错误日志输出的问题
System.setProperty("com.android.tools.r8.ignoreUnknownMetadataVersion", "true")

allprojects {
    repositories {
        apply(from = "${rootDir}/build_properties.gradle")
        val buildProperties = extensions.findByName("build_properties") as Map<String, Any?>
        mavenLocal()
        maven(url = "https://artifact.bytedance.com/repository/releases")
        buildProperties["custom_maven_url"]?.let {
            maven(url = it)
        }
        mavenCentral()
        google()
    }
}
