plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "com.example"
version = "1.0-SNAPSHOT"

base.archivesName.set("static-lib-demo")

kotlin {
    ohosArm64 {
        compilations {
            val main by getting {
                cinterops {
                    val mylib by creating {
                        defFile("$projectDir/nativeInterop/ohosArm64/mylib.def")
                        // .h and .a live in csrc/
                        extraOpts("-compiler-option", "-I$projectDir/csrc")
                        extraOpts("-libraryPath", "$projectDir/csrc")
                    }
                }
            }
        }
    }
    sourceSets {
        val ohosArm64Main by getting {
            dependencies {
                // depends on hilog-klib so the demo consumer can pull both via one coordinate
                implementation("com.example:hilog-klib:1.0-SNAPSHOT")
            }
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            artifactId = "static-lib-demo"
        }
    }
    // Publish to the repo-local Maven repo (kn_sample/m2, gitignored) instead of ~/.m2.
    // Use `./gradlew :static-lib-demo:publish`.
    repositories {
        maven { url = uri(rootProject.projectDir.parentFile.resolve("m2")) }
    }
}
