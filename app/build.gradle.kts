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
            executable {
                entryPoint = "com.example.main"
                val partialLinkMode = project.findProperty("partialLinkMode") as? String
                if (partialLinkMode != null) {
                    freeCompilerArgs += listOf("-Xpartial-linkage=$partialLinkMode")
                }
            }
        }
    }
    
    sourceSets {
        val commonMain by getting

        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation("com.example:dep-lib:1.0.0")
                implementation("com.example:caller-lib:1.0.0")
            }
        }
        
        val macosArm64Main by getting {
            dependsOn(nativeMain)
        }
    }
}
