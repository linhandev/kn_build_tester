plugins {
    kotlin("multiplatform") version "2.0.255-SNAPSHOT"
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64("bizA") {
        binaries {
            sharedLib {
                baseName = "bizA"
                freeCompilerArgs += listOf("-Xbinary=emitRuntime=true", "-Xbinary=splitBCfile=false")
            }
        }
    }

    ohosArm64("bizB") {
        binaries {
            sharedLib {
                baseName = "bizB"
                freeCompilerArgs += listOf("-Xbinary=emitRuntime=true", "-Xbinary=splitBCfile=false")
            }
        }
    }
    
    sourceSets {
        val bizAMain by getting {
            dependsOn(getByName("commonMain"))
            dependsOn(getByName("bizBMain"))
        }
        val bizBMain by getting {
            dependsOn(getByName("commonMain"))
            // dependsOn(getByName("bizAMain"))
        }
    }
}
