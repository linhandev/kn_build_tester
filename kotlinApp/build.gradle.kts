plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64 {
        binaries {
            sharedLib {
                baseName = "hellokn"
            }
        }
    }

    iosArm64 {
        binaries {
            framework {
                baseName = "HelloKN"
            }
        }
    }

    sourceSets {
        val ohosArm64Main by getting
        val iosArm64Main by getting
    }
}
