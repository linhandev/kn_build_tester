plugins {
//    kotlin("multiplatform") version "2.0.255-SNAPSHOT"
//    kotlin("multiplatform") version "2.2.0-ohos-06"
    kotlin("multiplatform") version "2.0.21-KBA-013"
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64("ohosArm64") {
        compilations.getByName("main") {
            cinterops {
                create("add") {
                    defFile("src/nativeInterop/add/add.def")
                    includeDirs("src/nativeInterop/add")
                }
            }
            defaultSourceSet.dependencies {
                implementation("com.example:multiply:1.0-SNAPSHOT")
            }
        }
        binaries {
            sharedLib {
                baseName = "c2k"
            }
        }
    }
    linuxX64("linuxX64") {
        compilations.getByName("main") {
            cinterops {
                create("add") {
                    defFile("src/nativeInterop/add/add.def")
                    includeDirs("src/nativeInterop/add")
                }
            }
            defaultSourceSet.dependencies {
                implementation("com.example:multiply:1.0-SNAPSHOT")
            }
        }
        binaries {
            sharedLib {
                baseName = "c2k"
            }
        }
    }
}

arrayOf("linkDebugSharedOhosArm64", "linkReleaseSharedOhosArm64", "linkDebugSharedLinuxX64", "linkReleaseSharedLinuxX64").map { it->
    tasks.named(it) {
        outputs.upToDateWhen { false }
    }
}
