plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "com.example"
version = "1.0.0"

kotlin {
    ohosArm64()
    iosArm64()
    
    sourceSets {
        val commonMain by getting
    }
}

publishing {
    repositories {
        maven {
            name = "local"
            url = uri("${rootProject.projectDir}/repo")
        }
    }
}
