@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

apply("${rootDir}/gradle/plugins/dependency-lock.gradle.kts")

android {
    namespace = "com.bytekmp.sample"
    //noinspection GradleDependency
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.bytekmp.sample"
        minSdk = libs.versions.android.minSdk.get().toInt()
        //noinspection ExpiredTargetSdkVersion
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation(libs.androidx.compose.activity)
    implementation(libs.bundles.coreCompose)
    implementation(libs.compose.resources)
    implementation(libs.compose.material)
    implementation(libs.compose.navigation)
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.spi)
    implementation(project(":launcher"))
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("androidx.appcompat:appcompat:1.3.1")
}
