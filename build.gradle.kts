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
                linkerOpts.add("-L${layout.buildDirectory.get().asFile.absolutePath}")
            }
        }
        
        compilations.getByName("main") {
            cinterops {
                val hello by creating {
                    defFile(project.file("src/nativeInterop/cinterop/hello.def"))
                    includeDirs("src/nativeInterop/c")

                    tasks.getByName(interopProcessingTaskName).dependsOn("compileHelloC")
                }
            }
        }
    }
    
    sourceSets {
        val nativeMain by getting
    }
}

tasks.register("compileHelloC") {
    val llvmPath = "/Users/user/.konan/dependencies/llvm-19.1.7-aarch64-macos-ohos-2"
    val sysrootPath = "/Users/user/.konan/dependencies/sysroot-ohos-aarch64-5.0.11.110"
    val clang = "$llvmPath/bin/clang"
    val cFile = file("src/nativeInterop/c/hello.c")
    val headerFile = file("src/nativeInterop/c/hello.h")
    val buildDir = layout.buildDirectory.get().asFile
    val objectFile = File(buildDir, "hello.o")
    val sharedLib = File(buildDir, "libhello.so")
    
    inputs.files(cFile, headerFile)
    outputs.files(sharedLib)
    
    doLast {
        buildDir.mkdirs()
        
        // First compile to object file
        project.exec {
            commandLine(
            clang,
            "--target=aarch64-linux-ohos",
            "-c",
            "-fPIC",
            cFile.absolutePath,
            "-o", objectFile.absolutePath,
            "-I${file("src/nativeInterop/c").absolutePath}"
          )
        }
        
        
        // Then create shared library from object file
        println(layout.buildDirectory.get().asFile.absolutePath)
        project.exec {
            commandLine(
                "$llvmPath/bin/ld.lld",
                "-shared",
                objectFile.absolutePath,
                "-o", sharedLib.absolutePath,
                "--sysroot", sysrootPath
            )
        }
    }
}
