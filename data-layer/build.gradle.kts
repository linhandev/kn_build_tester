plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.protobuf)
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
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.pbandk.runtime)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
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
                implementation(libs.ktor.client.okhttp)
            }
        }
        
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
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
                implementation(libs.ktor.client.okhttp)
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
    namespace = "com.knbuildtester.datalayer"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Protobuf configuration
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        create("pbandk") {
            artifact = "pro.streem.pbandk:protoc-gen-pbandk-jvm:${libs.versions.pbandk.get()}:jvm8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("pbandk") {
                    option("kotlin_package=com.knbuildtester.proto")
                }
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
                freeCompilerArgs.add("-Xdump-perf=${project.buildDir}/perf-dumps/data-layer-${target.name}-perf.txt")
                freeCompilerArgs.add("-Xprofile-phases")
            }
        }
        
        target.binaries.sharedLib {
            baseName = "data-layer"
            
            // LLVM optimization timing for data layer
            freeCompilerArgs += listOf(
                "-Xllvm-args=-time-passes",
                "-Xllvm-args=-stats-json=${project.buildDir}/llvm-stats/data-layer-${target.name}.json"
            )
        }
    }
}