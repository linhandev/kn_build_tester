plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0-SNAPSHOT"

val enableSplitBC: String? by project

kotlin {
    ohosArm64 {
        binaries {
            sharedLib {
                baseName = "repro"
                if (enableSplitBC == "true") {
                    freeCompilerArgs += "-Xbinary=splitBCfile=2"
                }
            }
        }
    }

    ohosX64 {
        binaries {
            sharedLib {
                baseName = "repro"
                if (enableSplitBC == "true") {
                    freeCompilerArgs += "-Xbinary=splitBCfile=2"
                }
            }
        }
    }

    iosArm64 {
        binaries {
            framework {
                baseName = "repro"
                if (enableSplitBC == "true") {
                    freeCompilerArgs += "-Xbinary=splitBCfile=2"
                }
            }
        }
    }

    macosArm64 {
        binaries {
            executable {
                baseName = "repro"
                if (enableSplitBC == "true") {
                    freeCompilerArgs += "-Xbinary=splitBCfile=2"
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting
    }
}
