buildscript {
    extra["kotlinVersion"] = "1.9.20"
}

plugins {
    kotlin("multiplatform") version "1.9.20" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}
