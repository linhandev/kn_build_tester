plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
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
        }
    }
    
    sourceSets {
        val commonMain by getting

        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("com.example:caller-lib:1.0.0")
                implementation(project(":src-lib"))
            }
        }
        
        val macosArm64Main by getting {
            dependsOn(nativeMain)
        }
    }
}
