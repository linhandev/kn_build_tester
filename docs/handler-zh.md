### MainHandler

提供往主线程执行任务的 API

##### runOnMainThread(block: () -> Unit)

在主线程执行任务

```kotlin
runOnMainThread {
    // 主线程执行代码
}
```

##### runOnMainThread(block: () -> Unit, delayMs: Long)

delay 执行主线程任务，单位毫秒

```kotlin
runOnMainThread({
    // 主线程延迟执行代码
}, 1000)
```

##### cancelBlock(block: () -> Unit)

取消 delay 的主线程任务
```kotlin
val block = {
    // 主线程执行代码
}
// 2000ms 后执行 block
runOnMainThread(block, 2000)
// 取消 2000ms 的 block
cancelBlock(block)
```

##### isMainThread()

是否主线程

