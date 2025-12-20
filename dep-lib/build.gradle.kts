plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "com.example"
version = "1.0.0"

kotlin {
    macosArm64()
    ohosArm64()
    iosArm64()
    
    sourceSets {
        val commonMain by getting
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}
