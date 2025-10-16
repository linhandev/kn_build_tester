plugins {
    kotlin("multiplatform") version "2.0.255-SNAPSHOT"
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64("withruntime") {
        binaries {
            sharedLib {
                baseName = "withruntime"
                freeCompilerArgs += listOf("-Xbinary=emitRuntime=true", "-Xbinary=splitBCfile=false")
                
            }
        }
    }
    
    ohosArm64("withoutruntime") {
        binaries {
            sharedLib {
                baseName = "withoutruntime"
                freeCompilerArgs += listOf("-Xbinary=emitRuntime=false", "-Xbinary=splitBCfile=false")
                linkerOpts("-L/Volumes/disk/git/sample/kn-sample/build/bin/withruntime/debugShared", "-lwithruntime")
            }
        }
    }
    
    sourceSets {
        val withruntimeMain by getting {
            dependsOn(getByName("commonMain"))
        }
        val withoutruntimeMain by getting {
            dependsOn(getByName("commonMain"))
        }
    }
}
