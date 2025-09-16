import org.gradle.api.tasks.Exec

plugins {
    kotlin("multiplatform") version "2.2.255-SNAPSHOT"
}


kotlin {
    val nativeTarget = ohosArm64("native") {
        compilations.all {
            compilerOptions.configure {
                freeCompilerArgs.add("-Xsave-llvm-ir-after=LinkBitcodeDependencies")
                freeCompilerArgs.add("-Xsave-llvm-ir-directory=${getLayout().buildDirectory.get()}")
            }
        }
        binaries {
            executable {
                entryPoint = "main"
                baseName = "kn-sample"
            }
        }
        compilations.getByName("main") { }
    }
    
    sourceSets {
        val nativeMain by getting
    }
}
