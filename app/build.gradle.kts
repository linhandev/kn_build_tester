plugins {
    kotlin("multiplatform")
}

kotlin {
    // JVM target
    jvm()
    
    // Native targets
    linuxX64("native") {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    
    macosX64("macos") {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    
    macosArm64("macosArm") {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":mathlib"))
                implementation(project(":stringlib"))
            }
        }
        
        // JVM-specific source set
        val jvmMain by getting {
            dependencies {
                implementation(project(":mathlib"))
                implementation(project(":stringlib"))
            }
        }
        
        // Share common code between all native targets
        val commonNative by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(project(":mathlib"))
                implementation(project(":stringlib"))
            }
        }
        
        // All native targets will automatically use commonNative
        targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
            compilations["main"].defaultSourceSet.dependsOn(commonNative)
        }
    }
}
