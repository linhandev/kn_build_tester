plugins {
    kotlin("multiplatform") version "2.0.21-KBA-013"
    //    kotlin("multiplatform") version "2.2.21-OH.0.1.0-01"
    `maven-publish`
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64("ohosArm64") {
        compilations.getByName("main") {
            cinterops {
                create("add") {
                    defFile("src/nativeInterop/add/add.def")
                    includeDirs("src/nativeInterop/add")
                }
            }
        }
    }
}

publishing {
    repositories {
        maven {
            name = "projectRepo"
            url = uri(File(rootProject.rootDir, "maven-repo").toURI())
        }
    }
}
