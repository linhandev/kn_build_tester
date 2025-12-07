package pkg

fun main() {
    // f() // This might be ambiguous if both are visible, but let's try to reproduce the linker error.
    // If I call f(), the compiler might complain about ambiguity before linking.
    // However, the user example has f() in main.
    // Let's assume the user knows what they are doing or maybe one takes precedence or it's a specific KN behavior.
    // To be safe and follow the user request exactly:
    f()
    println(getO() + getK())
}
