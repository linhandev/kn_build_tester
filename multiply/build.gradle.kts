plugins {
    kotlin("multiplatform") version "2.0.21-KBA-013"
    `maven-publish`
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64("ohosArm64") {
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
}
