plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    ohosArm64 {
        compilations.getByName("main") {
            cinterops {
                val cb by creating {
                    defFile(project.file("src/nativeInterop/cinterop/cb.def"))
                }
            }
        }
    }
}
