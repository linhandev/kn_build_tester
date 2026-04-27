@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

@CName("kn_helloworld")
fun helloworld(): String = error("Demo crash on start: uncaught Kotlin exception")

private fun libbacktraceLeaf(): Nothing = error("OHOS libbacktrace demo failure")

private fun libbacktraceMiddle(): Nothing = libbacktraceLeaf()

@CName("kn_libbacktrace_demo")
fun libbacktraceDemo(): String = try {
    libbacktraceMiddle()
} catch (e: Throwable) {
    buildString {
        appendLine("Captured Kotlin/Native stack trace:")
        e.getStackTrace().take(8).forEach { frame ->
            appendLine(frame)
        }
    }
}

@CName("kn_libbacktrace_crash")
fun libbacktraceCrash(): Unit {
    libbacktraceMiddle()
}
