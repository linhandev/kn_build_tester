plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()
    
    // Native targets for dynamic libraries
    linuxX64("native") {
        binaries {
            sharedLib()
        }
    }
    macosX64("macos") {
        binaries {
            sharedLib()
        }
    }
    macosArm64("macosArm") {
        binaries {
            sharedLib()
        }
    }
    
    sourceSets {
        val commonMain by getting
        val commonNative by creating {
            dependsOn(commonMain)
        }
        val nativeMain by getting {
            dependsOn(commonNative)
        }
        val macosMain by getting {
            dependsOn(commonNative)
        }
        val macosArmMain by getting {
            dependsOn(commonNative)
        }
        val jvmMain by getting
    }
}
