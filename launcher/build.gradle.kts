import org.gradle.kotlin.dsl.android

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.bytekmp.all)
    id("org.jetbrains.kotlin.native.cocoapods")
}

apply("${rootDir}/gradle/plugins/dependency-lock.gradle.kts")

apply("$rootDir/gradle/plugins/ohos_build.gradle")

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        // 配置framework
        it.binaries.framework {
            baseName = "launcher"
            isStatic = true
            // 配置需要导出到OC的工程
            export(project(":sample"))
        }
    }

    cocoapods {
        homepage = "something must not be null"
        summary = "something must not be null"
        version = "1.0"
        ios.deploymentTarget = "13.0"
        framework {
            baseName = "launcher"
            isStatic = true
            export(project(":sample"))
            linkerOpts.addAll(
                listOf(
                    "-framework", "UIKit",
                    "-framework", "CoreGraphics",
                    "-framework", "QuartzCore",
                    "-framework", "MobileCoreServices",
                    "-framework", "CoreTelephony",
                    "-framework", "SystemConfiguration",
                    "-framework", "CoreServices",
                    "-framework", "AVFoundation"
                )
            )
        }
    }

    applyDefaultHierarchyTemplate()
    sourceSets {

        commonMain.dependencies {
            implementation(libs.bundles.coreCompose)
            implementation(libs.compose.navigation)
            implementation(libs.compose.material)
            implementation(libs.kotlinx.coroutines)
            api(project(":sample"))
            implementation(project(":todo"))
        }

        getByName("ohosArm64Main") {
            dependencies {
                implementation(libs.kotlinx.coroutines)
                implementation(libs.ohos.ffi.annotation)
                implementation(libs.ohos.ffi.library)
                implementation(libs.compose.ohos.library)
                implementation(libs.compose.ohos.performance)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.disko)
            }
        }

        androidMain {
            // android dependencies
            dependencies {
                implementation(libs.androidx.compose.activity)
            }
        }
    }
}

android {
    namespace = "com.bytekmp.sample.launcher"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

bytekmp {
    isRootModule = true
    ffi.arkTs {
        packageOfGeneratedClass = "com.bytekmp.sample.launcher"
    }
    compose {
        enabled = true
        resources.enabled = true
    }
    spi {
        enabledTarget(android, ohosArm64, iosArm64, iosX64, iosSimulatorArm64)
    }
    publish.har {
        enabled = true
        name.nameOfHar = "@bytekmp/sample"
        name.nameOfKmpSo = project.name
        name.nameOfBridgeSo = "bytekmp_sample"
        version = "1.0.0"
        harmonyAppModuleName = "entry"
        outputDir = rootProject.file("app/ohosApp/.local_har")
        configureSharedLibs {
            freeCompilerArgs += "-Xdisable-phases=VerifyBitcode"
            freeCompilerArgs += "-Xexpect-actual-classes"
            freeCompilerArgs += "-Xruntime-logs=gc=info"
            freeCompilerArgs += "-Xbinary=gc=cms"
        }
        enablePerformanceProbe = true
    }
}
