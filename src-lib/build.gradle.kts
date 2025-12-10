plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0.0"

kotlin {
    macosArm64()
    
    sourceSets {
        val commonMain by getting
    }
}
