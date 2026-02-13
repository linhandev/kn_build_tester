plugins {
    kotlin("multiplatform") version "2.0.21-KBA-013"
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

val inRepoMaven = File(rootProject.rootDir, "maven-repo")
publishing {
    repositories {
        maven {
            name = "inRepo"
            url = uri(inRepoMaven.toURI())
        }
    }
}
