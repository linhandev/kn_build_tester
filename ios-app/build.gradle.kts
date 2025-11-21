plugins {
    kotlin("multiplatform")
}

kotlin {
    // iOS targets (for macOS hosts)
    iosArm64 {
        binaries {
            executable {
                entryPoint = "com.example.ios.main"
                // This is where we'll control partial linkage
                // freeCompilerArgs += listOf("-Xpartial-linkage=enable")
                // freeCompilerArgs += listOf("-Xpartial-linkage=disable")
            }
        }
    }
    
    iosSimulatorArm64 {
        binaries {
            executable {
                entryPoint = "com.example.ios.main"
                // This is where we'll control partial linkage
                // freeCompilerArgs += listOf("-Xpartial-linkage=enable")
                // freeCompilerArgs += listOf("-Xpartial-linkage=disable")
            }
        }
    }
    
    // Linux target (for testing on Linux hosts)
    linuxX64 {
        binaries {
            executable {
                entryPoint = "com.example.ios.main"
                // This is where we'll control partial linkage
                // freeCompilerArgs += listOf("-Xpartial-linkage=enable")
                // freeCompilerArgs += listOf("-Xpartial-linkage=disable")
            }
        }
    }
    
    sourceSets {
        val nativeMain by creating {
            dependencies {
                implementation(project(":dep-lib"))
                implementation(project(":caller-lib"))
            }
        }
        
        val iosMain by creating {
            dependsOn(nativeMain)
        }
        
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
        
        val linuxX64Main by getting {
            dependsOn(nativeMain)
        }
    }
}
