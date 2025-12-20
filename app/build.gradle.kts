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
