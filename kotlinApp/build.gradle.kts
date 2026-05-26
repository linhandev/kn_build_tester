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
}

tasks.findByName("linkDebugSharedOhosArm64")?.outputs?.upToDateWhen { false }
