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

val projectMavenRepo = File(rootProject.rootDir, "maven-repo")
val cinteropTask = tasks.named("cinteropAddOhosArm64")
val cinteropKlib: Provider<File> = cinteropTask.map {
    it.outputs.files.filter { f -> f.name.endsWith(".klib") && f.isFile }.single()
}

publishing {
    publications {
        create<MavenPublication>("addCinterop") {
            artifactId = "add-ohosarm64"
            groupId = "com.example"
            version = "1.0-SNAPSHOT"
            artifact(cinteropKlib) {
                extension = "klib"
            }
        }
    }
    repositories {
        maven {
            name = "projectRepo"
            url = uri(projectMavenRepo.toURI())
        }
    }
}

tasks.matching { it.name.startsWith("publish") && it.name.contains("AddCinterop") }.configureEach {
    dependsOn(cinteropTask)
}
