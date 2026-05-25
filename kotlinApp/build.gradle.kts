plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64 {
        binaries {
            sharedLib {
                baseName = "c2k"
                freeCompilerArgs += "-Xbinary=splitBCfile=2"
            }
        }
    }

    ohosX64 {
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

    macosArm64 {
        binaries {
            sharedLib {
                baseName = "c2k"
                freeCompilerArgs += "-Xbinary=splitBCfile=2"
            }
        }
    }
}
