import androidx.compose.runtime.Composable
import androidx.compose.material3.Text

// PUBLIC @Composable - FAILS at link time on OHOS arm64
@Composable
public fun PublicGreeting(name: String) {
    Text("Hello, $name!")
}

// INTERNAL @Composable - WORKS
@Composable
internal fun InternalGreeting(name: String) {
    Text("Hello internal, $name!")
}

// PRIVATE @Composable - WORKS
@Composable
private fun PrivateGreeting(name: String) {
    Text("Hello private, $name!")
}

// Public entry point - also FAILS
@Composable
public fun App() {
    PublicGreeting("World")
    InternalGreeting("World")
    PrivateGreeting("World")
}
