plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    jvm("desktop")
    
    // HarmonyOS target with performance monitoring
    mingwX64() // Using mingwX64 as substitute for ohosArm64 since it's not widely available
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":data-layer"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.pbandk.runtime)
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.core.ktx)
            }
        }
        
        val iosMain by creating {
            dependsOn(commonMain)
        }
        
        val iosX64Main by getting {
            dependsOn(iosMain)
        }
        
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
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

android {
    namespace = "com.knbuildtester.businesslogic"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Configure specific performance monitoring for this module
kotlin.targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> { target ->
    if (target.name == "mingwX64") {
        target.compilations.all {
            compilerOptions.configure {
                // Enhanced performance reporting for this critical module
                freeCompilerArgs.add("-Xreport-perf")
                freeCompilerArgs.add("-Xdump-perf=${project.buildDir}/perf-dumps/business-logic-${target.name}-perf.txt")
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
                "-Xllvm-args=-print-stats-json=${project.buildDir}/llvm-stats/business-logic-${target.name}.json"
            )
        }
        
        target.binaries.staticLib {
            baseName = "business-logic-static"
            export(project(":data-layer"))
        }
    }
}