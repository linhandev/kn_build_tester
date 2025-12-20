plugins {
    kotlin("multiplatform")
}

kotlin {
    macosArm64 {
        binaries {
            sharedLib {
                baseName = "app"
                val partialLinkMode = project.findProperty("partialLinkMode") as? String
                if (partialLinkMode != null) {
                    freeCompilerArgs += listOf("-Xpartial-linkage=$partialLinkMode")
                    freeCompilerArgs += listOf("-Xpartial-linkage-loglevel=warning")
                }
            }
            executable {
                entryPoint = "com.example.main"
                val partialLinkMode = project.findProperty("partialLinkMode") as? String
                if (partialLinkMode != null) {
                    freeCompilerArgs += listOf("-Xpartial-linkage=$partialLinkMode")
                    freeCompilerArgs += listOf("-Xpartial-linkage-loglevel=warning")
                }
            }
            sharedLib("check") {
                freeCompilerArgs += listOf("-produce", "header_cache", "-Xpartial-linkage=enable", "-Xpartial-linkage-loglevel=error")
                project.extensions.extraProperties["kotlin.native.cacheKind.macosArm64"] = "none"
            }
        }
    }
    ohosArm64 {
        binaries {
            sharedLib {
                baseName = "app"
                val partialLinkMode = project.findProperty("partialLinkMode") as? String
                if (partialLinkMode != null) {
                    freeCompilerArgs += listOf("-Xpartial-linkage=$partialLinkMode")
                    freeCompilerArgs += listOf("-Xpartial-linkage-loglevel=warning")
                }
            }
            sharedLib("check") {
                freeCompilerArgs += listOf("-produce", "header_cache", "-Xpartial-linkage=enable", "-Xpartial-linkage-loglevel=error", "-opt")
                project.extensions.extraProperties["kotlin.native.cacheKind.ohosArm64"] = "none"
            }
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":src-lib"))
            }
        }
    }
}
