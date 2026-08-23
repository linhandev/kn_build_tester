### JSValue

JSValue corresponds to any JS type and provides APIs to manipulate and convert JS objects, ***but*** can only be used in JS threads.

##### Convert Kotlin Value to JSValue

***Note: See "Type Conversion" section for supported types***
```kotlin
 // tid defaults to current thread, will crash if current thread is not JS thread
 val param1 = JSValue.createJSValue("param1", tid = mainTid)!!
```

##### Global Object

Get JS Global object, optional tid parameter, pass mainTid to execute on main thread, defaults to current thread (will crash if current thread is not JS thread)

```kotlin
    val global = JSValue.global(tid = getTid())
```

##### Create JS Object

Create a JS object that will be released when out of scope if no other references exist

Optional tid parameter, pass mainTid to execute on main thread, defaults to current thread (will crash if current thread is not JS thread)

```kotlin
    val jsObject = JSValue.createJSObject()
```

##### Multiple Callback Interface

For easier use of multiple callback interfaces, use KNCallback annotation which helps generate extension methods

```kotlin
@KNCallback
interface MultiCallbackListener {
    fun onSuccess(code: Int, response: String)
    fun onFail(code: Int)
    fun onStart(url: String, params: String)
    fun onEnd(url: String, params: String)
}

val jsvalue:JSValue = /** JSValue obtained from method or service **/
val listener:MultiCallbackListener = value.asMultiCallbackListener()
listener.onSuccess(200,"this is response.")
```

##### Array Operations

If JSValue is an array type, you can use operator directly for array operations

```kotlin
    val jsvalue:JSValue = /**js**/
    val params1 = jsvalue[0]
    jsvalue[0] = JSValue.createJSObject()
```

##### Object Operations

If JSValue is an Object type, you can perform property assignment and method calls

```kotlin
   val jsvalue:JSValue = /**js**/
   // Property read/write
   val value = jsvalue["key"]
   jsvalue["key"] = JSValue.createJSObject()
   
   // Method call
   // Call func method of jsvalue object with parameters: 1, 2, 3
   jsvalue.callMethod("func",1,2,3)
```

##### Type Conversion & Type Checking

toXXXX & isXXXX methods provide convenient APIs for type conversion and checking

```kotlin
    val jsvalue:JSValue = /**js**/
    if (jsvalue.isString()) {
        val str = jsvalue.toKString()
    }
    if (jsvalue.isObject()) {
       val map = jsvalue.toMap() 
    }
```

##### Get napi_value

You can get the corresponding napi_value through JSValue#handle, but must use it in the JS object's owning thread, Debug builds will throw exception if used in other threads

```kotlin
    val jsvalue:JSValue = /**js**/
    val handle:napi_value? = jsvalue.handle
```

##### Practical Examples

1. Get JSON under Global and call stringify

- Kotlin

```
val map = mapOf<String, Any?>("name" to "KMM", "arg" to 42)
val result = global["JSON"]?.callMethod("stringify", map)
info("stringify result = ${result?.toKString()}")
```

2. Multiple callback example

