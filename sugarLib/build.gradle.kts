plugins {
    kotlin("multiplatform") version "2.0.21-KBA-014"
    `maven-publish`
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64 {
        compilations.getByName("main") {
        }
    }
}
