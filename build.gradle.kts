import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform") version "2.0.255-SNAPSHOT"
    // kotlin("multiplatform") version "2.0.21-KBA-013"
}

group = "com.example"
version = "1.0-SNAPSHOT"

fun String.capitalizeFirst(): String = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

kotlin {
    ohosArm64("ohosArm64") {
        binaries {
            sharedLib {
                baseName = "c2k"
                linkerOpts += listOf("-z", "now")
                
                binaryOption("splitBCfile", "2")
                binaryOption("llvmSplitPath", "/Volumes/disk/git/llvm/third_party_llvm-project/build-used-02/bin/llvm-split")
            }
        }
        compilations.getByName("main") {
            cinterops {
                val znow_test by creating {
                    defFile(file("src/nativeMain/cinterop/znow_test.def"))
                    includeDirs(file("znow-test"))
                }
            }
        }
    }
    
    sourceSets {
        val ohosArm64Main by getting
    }
}
