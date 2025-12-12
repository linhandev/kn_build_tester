plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
    
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    ohosArm64() {
        binaries.sharedLib {
            baseName = "kn"
        }

        val main by compilations.getting

    }
    
    sourceSets {
        commonMain.dependencies {
            // No dependencies needed for basic arithmetic
        }
    }
}

android {
    namespace = "com.tencent.compose.sample.calculator"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}
