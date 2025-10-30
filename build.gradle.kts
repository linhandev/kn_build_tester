import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform") version "2.0.255-SNAPSHOT"
}

group = "com.example"
version = "1.0-SNAPSHOT"

fun String.capitalizeFirst(): String = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

kotlin {
    ohosArm64("ohosArm64") {
        binaries {
            sharedLib {
                baseName = "c2k"
                freeCompilerArgs += listOf("-Xexport-kdoc")
                linkerOpts += listOf("-z", "now")
                
                binaryOption("splitBCfile", "2")
                binaryOption("llvmSplitPath", "/Volumes/disk/git/llvm/third_party_llvm-project/build-used-02/bin/llvm-split")
            }
        }
        compilations.getByName("main") {
            cinterops {
                val san_test by creating {
                    defFile(file("src/nativeMain/cinterop/san_test.def"))
                    includeDirs(file("harmonyApp/entry/src/main/cpp/include"))
                }
            }
        }
    }
    
    sourceSets {
        val ohosArm64Main by getting
    }
}


arrayOf("debug", "release").forEach { type ->
    tasks.register<Copy>("publish${type.capitalizeFirst()}BinariesToHarmonyApp") {
        group = "harmony"
        dependsOn("link${type.capitalizeFirst()}SharedOhosArm64")
        into(rootProject.file("harmonyApp"))
        from("build/bin/ohosArm64/${type}Shared/libc2k_api.h") {
            into("entry/src/main/cpp/include/")
        }
        from(project.file("build/bin/ohosArm64/${type}Shared/libc2k.so")) {
            into("/entry/libs/arm64-v8a/")
        }
    }
}
