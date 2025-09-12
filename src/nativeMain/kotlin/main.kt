import hello.*
import kotlinx.cinterop.*

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun main() {
    // Call C function to add numbers
    val result = add_numbers(42, 58)
    println("Adding 42 + 58 = $result")
}
