plugins {
    kotlin("multiplatform")
}

kotlin {
    ohosArm64 {
        binaries {
            sharedLib {
                baseName = "app"
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

// Patch the lib klib manifest to add a bad dependency before linking
tasks.matching { it.name.contains("linkDebugSharedOhosArm64") || it.name == "linkDebugSharedOhosArm64" }.configureEach {
    dependsOn(":lib:compileKotlinOhosArm64")
    doFirst {
        val manifestFile = project(":lib").layout.buildDirectory.file("classes/kotlin/ohosArm64/main/klib/lib/default/manifest").get().asFile
        if (manifestFile.exists()) {
            val content = manifestFile.readText()
            val patched = content.replace("depends=stdlib", "depends=stdlib;org.jetbrains.kotlin.native.platform.ohos")
            manifestFile.writeText(patched)
            println("PATCHED manifest: $patched")
        } else {
            println("WARNING: manifest not found at ${manifestFile.absolutePath}")
        }
    }
}
