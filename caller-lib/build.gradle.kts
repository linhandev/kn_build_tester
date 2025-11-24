plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "com.example"
version = "1.0.0"

kotlin {
    macosArm64 {
        compilations["main"].compilerOptions.options.freeCompilerArgs.add("-Xpartial-linkage=enable")
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":dep-lib"))
            }
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}
