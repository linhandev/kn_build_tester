package stringlib

/**
 * Simple string library demonstrating dynamic library linking
 */
fun stringLibFunction(text: String): String {
    println("Called stringLibFunction from dynamic library")
    return "Processed: $text"
}
