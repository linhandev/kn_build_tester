plugins {
    kotlin("multiplatform")
}

kotlin {
    ohosArm64 {
        binaries {
            executable {
                entryPoint = "pkg.main"
                freeCompilerArgs += listOf("-g")
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
