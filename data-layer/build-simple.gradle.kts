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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
                implementation("pro.streem.pbandk:pbandk-runtime:0.14.2")
                implementation("io.ktor:ktor-client-core:3.0.2")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        
        val desktopMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:3.0.2")
            }
        }
        
        val mingwX64Main by getting {
            dependencies {
                // Platform specific dependencies
            }
        }
    }
}

// Configure performance monitoring for data layer
kotlin.targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> { target ->
    if (target.name == "mingwX64") {
        target.compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xreport-perf")
                freeCompilerArgs.add("-Xdump-perf=${project.layout.buildDirectory.get().asFile}/perf-dumps/data-layer-${target.name}-perf.txt")
                freeCompilerArgs.add("-Xprofile-phases")
            }
        }
        
        target.binaries.sharedLib {
            baseName = "data-layer"
            
            // LLVM optimization timing for data layer
            freeCompilerArgs += listOf(
                "-Xllvm-args=-time-passes",
                "-Xllvm-args=-stats-json=${project.layout.buildDirectory.get().asFile}/llvm-stats/data-layer-${target.name}.json"
            )
        }
    }
}