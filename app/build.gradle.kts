plugins {
    kotlin("multiplatform")
}

group = "com.example.cexport"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64 {
        binaries {
            sharedLib {
                baseName = "cexport"
            }
            staticLib {
                baseName = "cexport"
            }
        }
    }
    
    sourceSets {
        val ohosArm64Main by getting {
            dependencies {
                // Scenario 2: Dependency on subproject (submodule)
                implementation(project(":lib-subproject"))
                
                // Scenario 3: Dependency on klib module
                implementation(project(":lib-klib"))
            }
        }
    }
}
