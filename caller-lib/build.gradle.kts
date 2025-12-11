plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "com.example"
version = "1.0.0"

kotlin {
    macosArm64 {
        compilations["main"].compilerOptions.options.freeCompilerArgs.add("-Xpartial-linkage-loglevel=warning")
    }
    
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
