plugins {
    kotlin("multiplatform")
}

kotlin {
    ohosArm64 {
        binaries {
            executable {
                entryPoint = "pkg.main"
            }
        }
    }
    
    sourceSets {
        val ohosArm64Main by getting {
            dependencies {
                implementation(project(":m1"))
                implementation(project(":m2"))
            }
        }
    }
}
