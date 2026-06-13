plugins {
    kotlin("multiplatform") version "2.4.0"
}

kotlin {
    linuxX64 {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
    macosArm64 {
        binaries {
            executable {
                entryPoint = "main"
            }
        }
    }
}
