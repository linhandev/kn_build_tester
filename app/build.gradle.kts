plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0.0"

kotlin {
    ohosArm64 {
        binaries {
            sharedLib("app") {
                baseName = "app"
            }
        }
    }
    
    iosArm64 {
        binaries {
            framework {
                baseName = "App"
            }
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.example:lib:1.0.0")
            }
        }
    }
}
