import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}

subprojects {
    configurations.configureEach {
        val kotlinVersion = extensions.getByType<VersionCatalogsExtension>().named("libs")
            .findVersion("kotlin")
            .orElseThrow { IllegalStateException("kotlin version not found in version catalog") }
            .requiredVersion

        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(kotlinVersion)
                because("Align Kotlin artifacts with CPF compiler")
            }
        }
    }
}
