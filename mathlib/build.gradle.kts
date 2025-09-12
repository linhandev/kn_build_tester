plugins {
    kotlin("multiplatform")
}

kotlin {
    // JVM target
    jvm()
    
    // Native targets
    linuxX64("native") {
        binaries {
            staticLib {
                baseName = "mathlib"
            }
        }
    }
    
    macosX64("macos") {
        binaries {
            staticLib {
                baseName = "mathlib"
            }
        }
    }
    
    macosArm64("macosArm") {
        binaries {
            staticLib {
                baseName = "mathlib"
            }
        }
    }
    
    sourceSets {
        val commonMain by getting
        
        // JVM-specific source set
        val jvmMain by getting
        
        // Share common code between all native targets
        val commonNative by creating {
            dependsOn(commonMain)
        }
        
        // All native targets will automatically use commonNative
        targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
            compilations["main"].defaultSourceSet.dependsOn(commonNative)
        }
    }
}
