import androidx.compose.runtime.Composable

// PUBLIC @Composable — reported to FAIL with Kotlin 2.2.21-0.2.0-05
@Composable
fun PublicGreeting(name: String) {
    // Simple composable that should compile but reportedly fails when public
}

// PRIVATE @Composable — reportedly compiles fine
@Composable
private fun PrivateGreeting(name: String) {
    // Same implementation, just private
}

// INTERNAL @Composable — reportedly compiles fine
@Composable
internal fun InternalGreeting(name: String) {
    // Same implementation, just internal
}
