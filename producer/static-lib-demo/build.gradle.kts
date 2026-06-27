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
        named<MavenPublication>("ohosArm64") { artifactId = "static-lib-demo" }
        named<MavenPublication>("kotlinMultiplatform") { artifactId = "static-lib-demo-metadata" }
    }
}
