## Service Discovery

> For supported parameter and return value types and conversion rules, see the "Type Conversion" section

##### Kotlin Calling Kotlin Services

ServiceProvider can also expose interfaces to KMM layer, just need to bind and implement the interface

- 1. Declare API interface

```kotlin
// Declare interface and publish for implementation by dependents
interface TestServiceBApi {
    fun methodWithIntReturnInt(a: Int): Int
}

```

- 2. Implement API with ServiceProvider annotation and bind API interface

1. When interface has single implementation, can use bind and name parameters in ServiceProvider for binding
2. When interface has multiple implementations, can use name parameter in ServiceProvider for binding, cannot specify multiple
   binds (specifying multiple binds for same interface will crash and throw exception), can specify single bind

```kotlin
// Component internal implementation
@ServiceProvider(bind = TestServiceBApi::class)
open class TestServiceB : TestServiceBApi {

    // Pass Int return Int
    override fun methodWithIntReturnInt(a: Int): Int {
        return a + 1
    }
}

@ServiceProvider()
open class TestServiceC : TestServiceBApi {

    // Pass Int return Int
    override fun methodWithIntReturnInt(a: Int): Int {
        return a + 1
    }
}
```

- 3. Calling ServiceProvider in Kotlin

1. When interface has single implementation, can get via interface generic or name
2. When interface has multiple implementations, can get via interface generic or name, name takes priority, interface generic can only get bind-declared implementation

```kotlin
// KMM same-layer calling
KNOI.get<TestServiceBApi>().methodWithIntReturnInt(1) //Gets TestServiceB instance

KNOI.get<TestServiceBApi>("TestServiceB").methodWithIntReturnInt(1) //Gets TestServiceB instance

KNOI.get<TestServiceBApi>("TestServiceC").methodWithIntReturnInt(1) //Gets TestServiceC instance
```
