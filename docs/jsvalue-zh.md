### JSValue

JSValue 对应的是 JS 的任何类型，提供一些api，可操作转换 JS 对象，***但*** 仅仅在 JS 线程能使用。

##### Kotlin 值转 JSValue

***注：支持转换的类型见“类型转换章节”***
```kotlin
 // tid 不传默认为当前线程，如当前线程非JS 线程会崩溃
 val param1 = JSValue.createJSValue("param1", tid = mainTid)!!
```

##### Global 对象

获取 JS Global 对象，可选参数 tid，如期望回到主线程传入 mainTid，默认为当前线程（当前线程非 JS 线程时会崩溃）

```kotlin
    val global = JSValue.global(tid = getTid())
```

##### 创建 JS 对象

创建一个 JS 对象，该对象如无其他引用，退栈后会被释放掉

可选参数 tid，如期望回到主线程传入 mainTid，默认为当前线程（当前线程非 JS 线程时会崩溃）

```kotlin
    val jsObject = JSValue.createJSObject()
```

##### 多回调接口

为方便更方便使用多回调接口，可使用 KNCallback 注解，会辅助生成扩展方法

```kotlin
@KNCallback
interface MultiCallbackListener {
    fun onSuccess(code: Int, response: String)
    fun onFail(code: Int)
    fun onStart(url: String, params: String)
    fun onEnd(url: String, params: String)
}

val jsvalue:JSValue = /** 方法或服务获取的JSValue **/
val listener:MultiCallbackListener = value.asMultiCallbackListener()
listener.onSuccess(200,"this is response.")
```

##### 数组操作

如 JSValue 是数组类型，可使用 operator 直接进行数组操作

```kotlin
    val jsvalue:JSValue = /**js传入的类型**/
    val params1 = jsvalue[0]
    jsvalue[0] = JSValue.createJSObject()
```

##### 对象操作

如 JSValue 是 Object 类型，可进行属性赋值和方法调用

```kotlin
   val jsvalue:JSValue = /**js传入的类型**/
   // 属性读取和写入
   val value = jsvalue["key"]
   jsvalue["key"] = JSValue.createJSObject()
   
   //方法调用
   // 调用 jsvalue 对象的 func 方法，参数为：1，2，3 
   jsvalue.callMethod("func",1,2,3)
```

##### 类型转换 & 类型判断

toXXXX & isXXXX 系列方法提供了类型转换和类型判断便捷 API

```kotlin
    val jsvalue:JSValue = /**js传入**/
    if (jsvalue.isString()) {
        val str = jsvalue.toKString()
    }
    if (jsvalue.isObject()) {
       val map = jsvalue.toMap() 
    }
```

##### 获取 napi_value

可通过 JSValue#handle 获取到对应的 napi_value, 但需注意在 该 JS 对象所属线程中使用，如在其他线程使用该 api，Debug 包会主动抛异常

```kotlin
    val jsvalue:JSValue = /**js传入**/
    val handle:napi_value? = jsvalue.handle
```

##### 实践用例

1. 获取 Global 下的 JSON，并调用 stringify

- Kotlin

```
val map = mapOf<String, Any?>("name" to "KMM", "arg" to 42)
val result = global["JSON"]?.callMethod("stringify", map)
info("stringify result = ${result?.toKString()}")
```

2. 多回调实例

