## 服务发现

> 参数与返回值 支持类型及转换规则见 “类型转换” 章节

##### Kotlin 调用 Kotlin 服务

ServiceProvider 亦可暴露接口给 KMM 层，仅仅需绑定和实现接口即可

- 1. 声明 API 接口

```kotlin
// 声明接口，并发布提供给依赖方实现
interface TestServiceBApi {
    fun methodWithIntReturnInt(a: Int): Int
}

```

- 2. 实现 API 并增加 ServiceProvider 注解，并绑定 API 接口

1. 接口只有一个实现时注册，可以使用ServiceProvider中的 bind 参数和 name 参数进行绑定
2. 接口有多个实现时注册，可以使用ServiceProvider中的 name 参数进行绑定，此时不能指定多个 
bind(多个 bind 同时指定一个接口启动会 crash 并抛出异常) 可以指定单个 bind

```kotlin
// 组件内部实现
@ServiceProvider(bind = TestServiceBApi::class)
open class TestServiceB : TestServiceBApi {

    // 传入 Int 返回 Int
    override fun methodWithIntReturnInt(a: Int): Int {
        return a + 1
    }
}

@ServiceProvider()
open class TestServiceC : TestServiceBApi {

    // 传入 Int 返回 Int
    override fun methodWithIntReturnInt(a: Int): Int {
        return a + 1
    }
}
```

- 3. Kotlin 中调用 ServiceProvider

1. 接口只有一个实现时注册，可以通过接口泛型或者 name 获取
2. 接口有多个实现时注册，可以通过接口泛型或者 name 获取，有 name 优先通过 name 获取，接口泛型获取只能获取到 bind 声明的实现

```kotlin
// KMM 同层调用
KNOI.get<TestServiceBApi>().methodWithIntReturnInt(1) //此时获取到TestServiceB 实例

KNOI.get<TestServiceBApi>("TestServiceB").methodWithIntReturnInt(1) //此时获取到TestServiceB 实例

KNOI.get<TestServiceBApi>("TestServiceC").methodWithIntReturnInt(1) //此时获取到TestServiceC 实例
```
