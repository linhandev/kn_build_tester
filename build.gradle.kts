plugins {
    kotlin("multiplatform") version "2.2.255-SNAPSHOT"
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64("ohosArm64") {
        binaries {
            sharedLib {
                baseName = "c2k"
                freeCompilerArgs += listOf("-Xexport-kdoc")
            }
        }
    }
    
    sourceSets {
        val ohosArm64Main by getting
    }
}
