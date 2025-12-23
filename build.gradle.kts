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
                val add by creating {
                    defFile("src/nativeInterop/add/add.def")
                    includeDirs("src/nativeInterop/add")
                }
            }
            defaultSourceSet.dependencies {
                implementation(project(":multiply"))
            }
        }
        binaries {
            sharedLib {
                baseName = "c2k"
            }
        }
    }
}
