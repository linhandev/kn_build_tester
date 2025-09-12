package mathlib

actual fun getCurrentPlatform(): String = "JVM (Java ${System.getProperty("java.version")})"
