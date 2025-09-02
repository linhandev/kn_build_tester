plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
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
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(libs.androidx.lifecycle.viewmodel)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(project(":shared"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        
        val androidMain by getting {
            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)
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
                implementation(compose.desktop.currentOs)
            }
        }
        
        val mingwX64Main by getting {
            dependencies {
                // Native-specific dependencies
            }
        }
    }
}

android {
    namespace = "com.knbuildtester.composeapp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "com.knbuildtester.composeapp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}

compose.desktop {
    application {
        mainClass = "com.knbuildtester.composeapp.MainKt"

        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg, 
                         org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi, 
                         org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
            packageName = "com.knbuildtester.composeapp"
            packageVersion = "1.0.0"
        }
    }
}

// Enhanced performance monitoring for Compose app
kotlin.targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> { target ->
    if (target.name == "mingwX64") {
        target.compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xreport-perf")
                freeCompilerArgs.add("-Xdump-perf=${project.buildDir}/perf-dumps/compose-${target.name}-perf.txt")
                freeCompilerArgs.add("-Xprofile-phases")
            }
        }
        
        target.binaries.executable {
            entryPoint = "com.knbuildtester.composeapp.main"
            
            // Comprehensive LLVM timing for the main application binary
            freeCompilerArgs += listOf(
                "-Xllvm-args=-time-passes",
                "-Xllvm-args=-stats",
                "-Xllvm-args=-print-stats-json=${project.buildDir}/llvm-stats/compose-${target.name}.json",
                "-Xllvm-args=-print-module-scope",
                "-Xllvm-args=-print-after-all"
            )
        }
    }
}