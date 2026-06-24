import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.JsSourceMapEmbedMode
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kover)
}

group = "io.github.kotlin"
version = "1.0.0"

kotlin {
    jvm()

    androidLibrary {
        namespace = "org.jetbrains.kotlinx.multiplatform.library.template"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava() // enable java compilation support
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    // JS target — Node.js runtime, IR backend, source maps ON (needed for coverage remap).
    js {
        nodejs {
            // If NODE_V8_COVERAGE is set in the environment, propagate it to the node test process
            // so V8 writes coverage data. Gradle does NOT forward arbitrary env vars to forked node
            // by default, so this explicit pass-through is required for c8 coverage collection.
            testTask {
                val v8 = providers.environmentVariable("NODE_V8_COVERAGE").orNull
                if (v8 != null) environment("NODE_V8_COVERAGE", v8)
            }
        }
        compilerOptions {
            sourceMap = true
            sourceMapEmbedSources = JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS
        }
    }

    // Wasm target — Node.js runtime, source maps ON.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        nodejs {
            // Propagate NODE_V8_COVERAGE to the node test process (same as js target).
            testTask {
                val v8 = providers.environmentVariable("NODE_V8_COVERAGE").orNull
                if (v8 != null) environment("NODE_V8_COVERAGE", v8)
            }
        }
        compilerOptions {
            sourceMap = true
            sourceMapEmbedSources = JsSourceMapEmbedMode.SOURCE_MAP_SOURCE_CONTENT_ALWAYS
        }
    }

    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
