plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    ohosArm64 {
        binaries {
            sharedLib {
                baseName = "composeApp"
            }
        }
    }
    iosArm64 {
        binaries {
            framework {
                baseName = "ComposeApp"
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
        }
    }
}
