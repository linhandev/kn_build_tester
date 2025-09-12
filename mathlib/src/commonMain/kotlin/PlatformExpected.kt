package mathlib

expect fun getCurrentPlatform(): String

fun printPlatformInfo() {
    println("Running on: ${getCurrentPlatform()}")
}
