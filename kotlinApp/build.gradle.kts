plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64 {
        binaries {
            sharedLib {
                baseName = "splitbc_repro"
                freeCompilerArgs += "-Xbinary=splitBCfile=2"
            }
        }
    }
}
