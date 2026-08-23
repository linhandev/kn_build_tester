### 异常处理

##### ArkTS 异常 转 Kotlin 处理

在服务调用 & 方法调用 & JSValue 调用 ArkTS 方法时，如需要自行处理异常信息。可捕获 JavaScriptException

```kotlin
try {
    val noUseResult = getTestServiceAApi().methodWithException("methodWithException param1")
    info("TestServiceA methodWithException never Run this line.")
} catch (e: JavaScriptException) {
    // 获取 异常 message
    info("TestServiceA methodWithException: message = ${e.message}")
    // 获取 异常 TOP 堆栈
    info("TestServiceA methodWithException: JavaScriptTopStackTrace = ${e.getJavaScriptTopStackTrace()}")
    // 获取 异常 调用堆栈
    info("TestServiceA methodWithException: JavaScriptStackTrace = ${e.getJavaScriptStackTrace()?.joinToString("\n")}")
}
```

##### Kotlin 异常转 ArkTS 处理

施工中，有紧急需求可联系我
