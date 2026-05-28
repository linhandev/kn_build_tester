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
                // Controlled via -PsplitBC=0 or omitted for default
                val splitBC = findProperty("splitBC")
                if (splitBC != null) {
                    freeCompilerArgs += "-Xbinary=splitBCfile=$splitBC"
                }
            }
        }
    }

    iosArm64 {
        binaries {
            framework {
                baseName = "HelloKN"
                val splitBC = findProperty("splitBC")
                if (splitBC != null) {
                    freeCompilerArgs += "-Xbinary=splitBCfile=$splitBC"
                }
            }
        }
    }

    sourceSets {
        val ohosArm64Main by getting
        val iosArm64Main by getting
    }
}
