plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kover) apply false
}

// Unify yarn lock storage for both js and wasmJs targets.
// By default they use different lock directories (kotlin-js-store vs build/wasm), which breaks
// kotlinWasmStoreYarnLock when both targets coexist. Pin them to one directory.
allprojects {
    plugins.withId("org.jetbrains.kotlin.gradle.targets.js.yarn") {
        extensions.configure<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension> {
            lockFileDirectory = rootDir.resolve("kotlin-js-store")
        }
    }
}
