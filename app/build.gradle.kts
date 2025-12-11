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
            executable("plCheck") {
                entryPoint = "com.example.main"
                freeCompilerArgs += listOf("-produce", "header_cache", "-Xpartial-linkage=enable", "-Xpartial-linkage-loglevel=error")
                project.extensions.extraProperties.set("kotlin.native.cacheKind", "none")
            }
        }
    }
    
    sourceSets {
        val commonMain by getting

        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(project(":src-lib"))
            }
        }
        
        val macosArm64Main by getting {
            dependsOn(nativeMain)
        }
    }
}
