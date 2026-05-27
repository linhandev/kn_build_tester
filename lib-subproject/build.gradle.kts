plugins {
    kotlin("multiplatform")
}

group = "com.example.cexport"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64()
    
    sourceSets {
        val ohosArm64Main by getting
    }
}
