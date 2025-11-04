plugins {
    kotlin("multiplatform")
}

kotlin {
    ohosArm64("ohosArm64")

    sourceSets {
        val ohosArm64Main by getting
    }
}
