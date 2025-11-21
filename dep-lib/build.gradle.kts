plugins {
    kotlin("multiplatform")
}

kotlin {
    // iOS targets (for macOS hosts)
    iosArm64()
    iosSimulatorArm64()
    
    // Linux target (for testing on Linux hosts)
    linuxX64()
    
    sourceSets {
        val commonMain by getting
    }
}
