@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

import platform.posix.SIGSEGV
import platform.posix.raise

private interface CrashStep {
    fun apply(input: Int): Int
}

private class AddStep(private val delta: Int) : CrashStep {
    override fun apply(input: Int): Int = input + delta
}

private class MultiplyStep(private val factor: Int) : CrashStep {
    override fun apply(input: Int): Int = input * factor
}

private fun buildSignature(seed: Int): Int {
    val steps: List<CrashStep> = listOf(
        AddStep(11),
        MultiplyStep(3),
        AddStep(5),
        MultiplyStep(2),
    )
    var value = seed
    for (step in steps) {
        value = step.apply(value)
    }
    // Force a synchronous native crash while still inside buildSignature.
    raise(SIGSEGV)
    return value
}

private fun buildCrashPayload(signature: Int): String {
    val fields = linkedMapOf(
        "stage" to "startup",
        "seed" to "7",
        "signature" to signature.toString(),
    )
    return fields.entries.joinToString(separator = ", ") { (key, value) -> "$key=$value" }
}

@CName("kn_helloworld")
fun helloworld() {
    println("Hello World")
    val signature = buildSignature(seed = 7)
    throw IllegalStateException("Intentional startup crash for hstack demo: ${buildCrashPayload(signature)}")
}
