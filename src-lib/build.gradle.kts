plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0.0"

kotlin {
    macosArm64()
    ohosArm64()
    iosArm64()
    
    sourceSets {
        val commonMain by getting 
        val nativeMain by creating {
            dependencies {
                implementation("com.example:caller-lib:1.0.0")
            }
        }
    }
}
