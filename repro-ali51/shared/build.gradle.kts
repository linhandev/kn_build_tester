import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // iOS arm64 framework target
    iosArm64 {
        binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    // OHOS arm64 shared lib target
    ohosArm64 {
        binaries.sharedLib {
            baseName = "shared"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
        }
    }
}
