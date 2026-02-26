plugins {
    // kotlin("multiplatform") version "2.0.21-KBA-014"
kotlin("multiplatform") version "2.2.21-OH.0.1.0-01"
    `maven-publish`
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64 {
        compilations.getByName("main") {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xklib-enable-signature-clash-checks=false")
            }
        }
    }
    iosArm64 {
        compilations.getByName("main") {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xklib-enable-signature-clash-checks=false")
            }
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}
