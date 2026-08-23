package com.bytekmp.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.artifacts.DependencyResolveDetails
import java.io.File
import java.util.Properties
import org.gradle.kotlin.dsl.maven

class ByteKmpSettingsPlugin : Plugin<Settings> {
    override fun apply(settings: Settings) {
        val gradle = settings.gradle
        val rootDir = settings.rootDir

        val isIOS = isIOS(gradle)
        val properties = loadProperties(rootDir)

        // Configure Plugin Management
        settings.pluginManagement {
            val kotlinVersion = if (isIOS) {
                properties.getProperty("kotlin.version.ios")
            } else {
                properties.getProperty("kotlin.version.non-ios")
            }

            val kspVersion = if (isIOS) {
                properties.getProperty("ksp.version.ios")
            } else {
                properties.getProperty("ksp.version.non-ios")
            }

            resolutionStrategy {
                eachPlugin {
                    if (requested.id.id.startsWith("org.jetbrains.kotlin")) {
                        useVersion(kotlinVersion)
                    }
                    if (requested.id.id == "com.google.devtools.ksp") {
                        useVersion(kspVersion)
                    }
                }
            }
        }

        // Configure All Projects for Dependency Resolution
        gradle.allprojects {
            configurations.all {
                resolutionStrategy {
                    eachDependency {
                        when {
                            requested.group == "org.jetbrains.compose.components" && requested.name.startsWith("components-resources") -> {
                                useVersion(getComposeVersion(isIOS, properties, "resource"))
                            }
                            requested.group.startsWith("org.jetbrains.compose") -> {
                                useVersion(getComposeVersion(isIOS, properties, "common"))
                            }
                            requested.group.startsWith("org.jetbrains.androidx.lifecycle") -> {
                                useVersion(getComposeVersion(isIOS, properties, "lifecycle"))
                            }
                            requested.group.startsWith("org.jetbrains.androidx.navigation") -> {
                                useVersion(getComposeVersion(isIOS, properties, "navigation"))
                            }
                        }
                    }
                    // Exclude Skiko
                    exclude(mapOf("group" to "org.jetbrains.skiko"))
                }
            }
        }
    }

    private fun isIOS(gradle: Gradle): Boolean {
        return gradle.startParameter.taskNames.any {
            it.contains("ios", ignoreCase = true) || it.contains("syncFramework")
        }
    }

    private fun loadProperties(rootDir: File): Properties {
        val properties = Properties()
        val propertyFile = File(rootDir, "gradle.properties")
        if (propertyFile.exists()) {
            propertyFile.inputStream().use { properties.load(it) }
        }
        return properties
    }

    private fun getComposeVersion(isIos: Boolean, properties: Properties, type: String): String {
        val key = if (isIos) {
            "compose.$type.ios.version"
        } else {
            "compose.$type.non-ios.version"
        }
        return properties.getProperty(key) ?: error("Property $key not found in gradle.properties")
    }
}
