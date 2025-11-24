buildscript {
    extra["kotlinVersion"] = "2.0.21"
}

plugins {
    kotlin("multiplatform") version "2.0.21" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}
