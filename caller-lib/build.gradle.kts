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
        val commonMain by getting {
            dependencies {
                implementation("com.example:dep-lib:1.0.0")
            }
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}
