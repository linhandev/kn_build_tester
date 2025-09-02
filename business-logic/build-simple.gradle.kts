plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm("desktop")
    mingwX64()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":data-layer"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
                implementation("pro.streem.pbandk:pbandk-runtime:0.14.2")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        
        val desktopMain by getting {
            dependencies {
                // Desktop specific dependencies
            }
        }
        
        val mingwX64Main by getting {
            dependencies {
                // Platform specific dependencies for native target
            }
        }
    }
}

// Configure specific performance monitoring for this module
kotlin.targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> { target ->
    if (target.name == "mingwX64") {
        target.compilations.all {
            compilerOptions.configure {
                // Enhanced performance reporting for this critical module
                freeCompilerArgs.add("-Xreport-perf")
                freeCompilerArgs.add("-Xdump-perf=${project.layout.buildDirectory.get().asFile}/perf-dumps/business-logic-${target.name}-perf.txt")
                freeCompilerArgs.add("-Xprofile-phases")
                freeCompilerArgs.add("-Xtime")
            }
        }
        
        target.binaries.sharedLib {
            baseName = "business-logic"
            export(project(":data-layer"))
            
            // Configure LLVM optimization timing
            freeCompilerArgs += listOf(
                "-Xllvm-args=-time-passes",
                "-Xllvm-args=-stats",
                "-Xllvm-args=-print-module-scope",
                "-Xllvm-args=-print-stats-json=${project.layout.buildDirectory.get().asFile}/llvm-stats/business-logic-${target.name}.json"
            )
        }
        
        target.binaries.staticLib {
            baseName = "business-logic-static"
            export(project(":data-layer"))
        }
    }
}