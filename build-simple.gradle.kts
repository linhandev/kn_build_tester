plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

// Configure performance reporting for all subprojects
subprojects {
    afterEvaluate {
        extensions.findByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()?.let { kotlin ->
            kotlin.targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> { target ->
                target.compilations.all {
                    compilerOptions.configure {
                        // Performance reporting
                        freeCompilerArgs.add("-Xreport-perf")
                        freeCompilerArgs.add("-Xdump-perf=${project.buildDir}/perf-dumps/${target.name}-perf-dump.txt")
                        freeCompilerArgs.add("-Xprofile-phases")
                        
                        // Enable timing for LLVM passes
                        freeCompilerArgs.add("-Xllvm-variant=dev")
                    }
                }
                
                target.binaries.all {
                    // Configure LLVM timing
                    freeCompilerArgs += listOf(
                        "-Xllvm-args=-time-passes",
                        "-Xllvm-args=-stats",
                        "-Xllvm-args=-print-stats-json=${project.buildDir}/llvm-stats/${target.name}-${name}.json"
                    )
                }
            }
        }
    }
}

// Task to generate build timing report
tasks.register("generateBuildReport") {
    group = "reporting"
    description = "Generate comprehensive build timing report"
    
    doLast {
        val reportDir = file("$buildDir/reports/build-timing")
        reportDir.mkdirs()
        
        val reportFile = file("$reportDir/build-timing-report.md")
        reportFile.writeText("""
# Kotlin Native Build Performance Report

Generated at: ${java.time.LocalDateTime.now()}

## Project Configuration
- Scale Classes: ${project.findProperty("project.scale.classes") ?: "100"}
- Scale Functions: ${project.findProperty("project.scale.functions") ?: "50"}
- Scale Protobuf Messages: ${project.findProperty("project.scale.protobuf.messages") ?: "20"}

## Performance Dumps Location
- Frontend/Backend timing: `build/perf-dumps/`
- LLVM stats: `build/llvm-stats/`

## Key Metrics to Analyze
1. Frontend compilation time
2. Backend compilation time
3. ModuleBitcodeOptimization duration
4. LTOBitcodeOptimization duration
5. Individual LLVM pass timings

""")
        println("Build report generated at: ${reportFile.absolutePath}")
    }
}