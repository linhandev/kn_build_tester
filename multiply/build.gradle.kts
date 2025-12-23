plugins {
    kotlin("multiplatform") version "2.3.0"
    `maven-publish`
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    linuxX64("linuxX64") {
        compilations.getByName("main") {
            cinterops {
                create("multiplyciopkg") {
                    defFile("src/nativeInterop/multiply/multiply.def")
                    includeDirs("src/nativeInterop/multiply")
                }
            }
        }
        binaries {
            sharedLib {
                baseName = "multiply"
            }
        }
    }
    
    iosSimulatorArm64("iosSimulatorArm64") {
        compilations.getByName("main") {
            cinterops {
                create("multiplyciopkg") {
                    defFile("src/nativeInterop/multiply/multiply.def")
                    includeDirs("src/nativeInterop/multiply")
                }
            }
        }
        binaries {
            framework {
                baseName = "multiply"
            }
        }
    }
}
