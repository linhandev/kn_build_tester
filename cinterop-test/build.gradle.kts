plugins {
    kotlin("multiplatform")
}

kotlin {
    ohosArm64 {
        compilations.getByName("main") {
            cinterops {
                create("test")
            }
        }
    }

    sourceSets {
        val ohosArm64Main by getting {
            dependencies {
                implementation(project(":lib"))
            }
        }
    }
}

// Patch the lib klib manifest to add a bad dependency before any cinterop task runs
gradle.projectsEvaluated {
    tasks.matching { it.name.lowercase().contains("cinterop") && it.name.lowercase().contains("ohos") }.configureEach {
        doFirst {
            val manifestFile = project(":lib").layout.buildDirectory.file("classes/kotlin/ohosArm64/main/klib/lib/default/manifest").get().asFile
            if (manifestFile.exists()) {
                val content = manifestFile.readText()
                if (!content.contains("org.jetbrains.kotlin.native.platform.ohos")) {
                    val patched = content.replace("depends=stdlib", "depends=stdlib org.jetbrains.kotlin.native.platform.ohos")
                    manifestFile.writeText(patched)
                    println("PATCHED manifest for cinterop test")
                }
            }
        }
    }
}
