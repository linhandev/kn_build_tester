plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm("desktop")
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

tasks.register<JavaExec>("runDemo") {
    group = "application"
    description = "Run the KN Build Tester demo"
    
    dependsOn("desktopJar")
    classpath = configurations.getByName("desktopRuntimeClasspath") + tasks.getByName("desktopJar").outputs.files
    mainClass.set("com.knbuildtester.composeapp.MainKt")
}