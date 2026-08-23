### Exception Handling

##### Handling ArkTS Exceptions in Kotlin

When making service calls, method calls, or calling ArkTS methods via JSValue, you can catch JavaScriptException to handle exception information.

```kotlin
try {
    val noUseResult = getTestServiceAApi().methodWithException("methodWithException param1")
    info("TestServiceA methodWithException never Run this line.")
} catch (e: JavaScriptException) {
    // Get exception message
    info("TestServiceA methodWithException: message = ${e.message}")
    // Get top exception stack trace
    info("TestServiceA methodWithException: JavaScriptTopStackTrace = ${e.getJavaScriptTopStackTrace()}")
    // Get full exception call stack
    info("TestServiceA methodWithException: JavaScriptStackTrace = ${e.getJavaScriptStackTrace()?.joinToString("\n")}")
}
```

##### Handling Kotlin Exceptions in ArkTS

//TODO