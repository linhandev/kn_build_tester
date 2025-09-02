plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm("desktop")
    androidNativeArm64()
    iosArm64()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":business-logic"))
                api(project(":data-layer"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
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
        
        val androidNativeArm64Main by getting {
            dependencies {
                // Platform specific dependencies
            }
        }
        
        val iosArm64Main by getting {
            dependencies {
                // Platform specific dependencies
            }
        }
    }
}

// Configure shared library exports for native targets
kotlin.targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> { target ->
    if (target.name == "androidNativeArm64" || target.name == "iosArm64") {
        target.compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xreport-perf")
                freeCompilerArgs.add("-Xdump-perf=${project.layout.buildDirectory.get().asFile}/perf-dumps/shared-${target.name}-perf.txt")
                freeCompilerArgs.add("-Xprofile-phases")
            }
        }
        
        target.binaries.sharedLib {
            baseName = "kn-shared"
            export(project(":business-logic"))
            export(project(":data-layer"))
            
            // Configure for comprehensive performance analysis
            freeCompilerArgs += listOf(
                "-Xllvm-args=-time-passes",
                "-Xllvm-args=-stats",
                "-Xllvm-args=-print-stats-json=${project.layout.buildDirectory.get().asFile}/llvm-stats/shared-${target.name}.json",
                "-Xllvm-args=-print-module-scope"
            )
        }
        
        target.binaries.staticLib {
            baseName = "kn-shared-static"
            export(project(":business-logic"))
            export(project(":data-layer"))
        }
    }
}