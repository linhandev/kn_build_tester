plugins {
    kotlin("multiplatform")
}

kotlin {
    ohosArm64("ohosArm64") {
        binaries {
            sharedLib {
                baseName = "bizA"
            }
        }
    }

    sourceSets {
        val ohosArm64Main by getting {
            dependencies {
                implementation(project(":bizB"))
            }
        }
    }
}
