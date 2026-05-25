plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    // All targets configured with splitBCfile=2 to reproduce the crash
    ohosArm64 {
        binaries {
            sharedLib {
                baseName = "c2k"
                freeCompilerArgs += "-Xbinary=splitBCfile=2"
            }
        }
    }

    iosArm64 {
        binaries {
            sharedLib {
                baseName = "c2k"
                freeCompilerArgs += "-Xbinary=splitBCfile=2"
            }
        }
    }

    linuxX64 {
        binaries {
            executable {
                baseName = "c2k"
                freeCompilerArgs += "-Xbinary=splitBCfile=2"
            }
        }
    }

    macosX64 {
        binaries {
            executable {
                baseName = "c2k"
                freeCompilerArgs += "-Xbinary=splitBCfile=2"
            }
        }
    }
}
