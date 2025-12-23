plugins {
    kotlin("multiplatform") version "2.3.0"
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
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
    
    iosSimulatorArm64("iosSimulatorArm64") {
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
            framework {
                baseName = "c2k"
            }
        }
    }
}

arrayOf("linkDebugSharedLinuxX64", "linkReleaseSharedLinuxX64", "linkDebugFrameworkIosSimulatorArm64", "linkReleaseFrameworkIosSimulatorArm64").map { it->
    tasks.named(it) {
        outputs.upToDateWhen { false }
    }
}
