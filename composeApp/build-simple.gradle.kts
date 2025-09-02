plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm("desktop")
    mingwX64()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
            }
        }
        
        val desktopMain by getting {
            dependencies {
                // Desktop specific dependencies
            }
        }
        
        val mingwX64Main by getting {
            dependencies {
                // Native-specific dependencies
            }
        }
    }
}

// Enhanced performance monitoring for Compose app
kotlin.targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> { target ->
    if (target.name == "mingwX64") {
        target.compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xreport-perf")
                freeCompilerArgs.add("-Xdump-perf=${project.layout.buildDirectory.get().asFile}/perf-dumps/compose-${target.name}-perf.txt")
                freeCompilerArgs.add("-Xprofile-phases")
            }
        }
        
        target.binaries.executable {
            entryPoint = "com.knbuildtester.composeapp.main"
            
            // Comprehensive LLVM timing for the main application binary
            freeCompilerArgs += listOf(
                "-Xllvm-args=-time-passes",
                "-Xllvm-args=-stats",
                "-Xllvm-args=-print-stats-json=${project.layout.buildDirectory.get().asFile}/llvm-stats/compose-${target.name}.json",
                "-Xllvm-args=-print-module-scope",
                "-Xllvm-args=-print-after-all"
            )
        }
    }
}