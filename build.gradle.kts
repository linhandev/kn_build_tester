plugins {
    kotlin("multiplatform") version "2.0.21-KBA-013"
    //     kotlin("multiplatform") version "2.2.21-OH.0.1.0-01"
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64("ohosArm64") {
        compilations.getByName("main") {
            defaultSourceSet.dependencies {
                implementation("com.example:add-ohosarm64:1.0-SNAPSHOT")
            }
        }
        binaries {
            sharedLib {
                baseName = "c2k"
                freeCompilerArgs += "-Xverbose-phases=Linker"
            }
        }
    }
}

arrayOf("linkDebugSharedOhosArm64", "linkReleaseSharedOhosArm64").forEach {
    tasks.named(it) {
        outputs.upToDateWhen { false }
    }
}
