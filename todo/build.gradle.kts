@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.bytekmp.all)
    id("org.jetbrains.kotlin.plugin.serialization")
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
    )

    applyDefaultHierarchyTemplate()
    sourceSets {

        commonMain.dependencies {
            implementation(libs.bundles.coreCompose)
            implementation(libs.compose.navigation)
            implementation(libs.compose.material)
            implementation(libs.kotlinx.coroutines)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.spi)
            implementation(libs.ohos.ffi.annotation)
        }

        getByName("ohosArm64Main") {
            // native dependencies
            dependencies {
                implementation(libs.kotlinx.coroutines)
                implementation(libs.ohos.ffi.library)
                implementation(libs.compose.ohos.library)
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
    namespace = "com.bytekmp.sample.todo"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

bytekmp {
    ffi.arkTs {
        packageOfGeneratedClass = "com.bytekmp.sample.todo"
    }
    compose {
        enabled = true
        resources.enabled = true
    }
    spi {
        enabledTarget(android, ohosArm64, iosArm64, iosX64, iosSimulatorArm64)
    }
}
