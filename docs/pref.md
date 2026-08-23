# KNOI Thread Selection Decision Table for Services

| Calling Thread (Kotlin) | Registered Thread (ArkTS) | Thread Switch Required | Final Decision Thread | Notes |
| --- | --- | --- | --- | --- |
| Main Thread | Main Thread | No | Main Thread | Direct call without thread switching |
| Main Thread | Worker Thread | Prohibited | Main Thread | Main thread must call services registered on main thread |
| Sub Thread | Main Thread | Yes | Main Thread | Use main thread instance if no worker thread instance registered |
| Sub Thread | Worker Thread | Yes | Worker Thread | Load balancing only effective in this case |

**Note: Load balancing for KNOI services is only possible when both caller and callee can execute in non-main threads**

**Note: After load balancing, singleton will become dual instances - one for main thread and one shared by all worker threads**

**Note: After load balancing, singleton will become dual instances - one for main thread and one shared by all worker threads**

**Note: After load balancing, singleton will become dual instances - one for main thread and one shared by all worker threads**

# KNOI Performance Optimization Best Practices

## 0. Direct C Code Calls
For interfaces already supported by Kotlin Native or Harmony C layer bridging, call the corresponding C code directly from Kotlin side, such as logging and timestamps.

## 1. Calling Thread Optimization
If both caller and callee can execute in worker threads, **it's recommended to register ArkTS implementations in worker threads (in addition to mandatory main thread registration)**. KNOI service discovery will automatically perform load balancing.

## 2. Value and Callback Caching on Kotlin Side
1. For values that don't change on ArkTS side, cache them on Kotlin side using singleton or other patterns after getting via getApi service to avoid KNOI bridging in subsequent calls, significantly improving performance.
2. For changing values or listeners/callbacks on ArkTS side, avoid managing observers/callbacks on ArkTS side. Best practice is to bind only one KNOI listener callback and handle observer notifications on Kotlin side to prevent performance degradation when callbacks/observers increase.

## 3. KNOI Service Single Responsibility Principle
1. Services should preferably be stateless pure functions, with caching and state management implemented on Kotlin side. This greatly reduces cross-runtime call blocking and, combined with caching, significantly mitigates performance issues for non-initial calls.
