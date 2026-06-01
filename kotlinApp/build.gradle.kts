plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0-SNAPSHOT"

// Read splitBCfile level from project property (default: 2)
val splitBCLevel: String by project

kotlin {
    // Target 1: OHOS arm64
    ohosArm64 {
        binaries {
            sharedLib {
                baseName = "splitbc_test"
                freeCompilerArgs += "-Xbinary=splitBCfile=$splitBCLevel"
            }
        }
    }

    // Target 2: OHOS x64
    ohosX64 {
        binaries {
            sharedLib {
                baseName = "splitbc_test"
                freeCompilerArgs += "-Xbinary=splitBCfile=$splitBCLevel"
            }
        }
    }

    // Target 3: iOS arm64
    iosArm64 {
        binaries {
            sharedLib {
                baseName = "splitbc_test"
                freeCompilerArgs += "-Xbinary=splitBCfile=$splitBCLevel"
            }
        }
    }

    // Target 4: macOS arm64 (host, no cross-compile)
    macosArm64 {
        binaries {
            sharedLib {
                baseName = "splitbc_test"
                freeCompilerArgs += "-Xbinary=splitBCfile=$splitBCLevel"
            }
        }
    }

    sourceSets {
        val commonMain by getting
    }
}
