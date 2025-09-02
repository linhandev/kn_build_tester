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
    mingwX64()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":business-logic"))
                api(project(":data-layer"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
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
                // Platform specific dependencies
            }
        }
    }
}

android {
    namespace = "com.knbuildtester.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Configure shared library exports for native targets
kotlin.targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> { target ->
    if (target.name == "mingwX64") {
        target.compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xreport-perf")
                freeCompilerArgs.add("-Xdump-perf=${project.buildDir}/perf-dumps/shared-${target.name}-perf.txt")
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
                "-Xllvm-args=-print-stats-json=${project.buildDir}/llvm-stats/shared-${target.name}.json",
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