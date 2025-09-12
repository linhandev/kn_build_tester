package mathlib

/**
 * Simple math library demonstrating static library linking
 */
fun mathLibAddFunction(x: Int, y: Int): Int {
    println("Called mathLibAddFunction from static library")
    return x + y
}
