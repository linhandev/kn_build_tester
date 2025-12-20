plugins {
    kotlin("multiplatform") version "2.0.21-KBA-014"
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64 {
        binaries {
            sharedLib {
                baseName = "c2k"
            }
        }
    }
    
    sourceSets {
        val ohosArm64Main by getting
    }
}
