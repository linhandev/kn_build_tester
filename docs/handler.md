### MainHandler

Provides APIs for executing tasks on the main thread

##### runOnMainThread(block: () -> Unit)

Execute task on main thread

```kotlin
runOnMainThread {
    // Code to execute on main thread
}
```

##### runOnMainThread(block: () -> Unit, delayMs: Long)

Execute task on main thread with delay (in milliseconds)

```kotlin
runOnMainThread({
    // Code to execute on main thread with delay
}, 1000)
```

##### cancelBlock(block: () -> Unit)

Cancel delayed main thread task
```kotlin
val block = {
    // Code to execute on main thread
}
// Execute block after 2000ms
runOnMainThread(block, 2000)
// Cancel the 2000ms delayed block
cancelBlock(block)
```

##### isMainThread()

Check if current thread is main thread
