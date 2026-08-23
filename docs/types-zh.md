### 类型转换

***所有方法调用、服务调用均只支持下表中的类型***

由于 Kotlin Native 与 ArkTs 类型无法完全匹配，在跨 Runtime 调用时会有类型转换，转换规则如下：

| **JavaScript**               | **Kotlin** |
|------------------------------| ------------------------ |
| Boolean                      | Boolean
| Number                       | Double、Int、Long |
| String                       | String               |
| Array                        | Array、List           |
| Function                     | (args: Array<JSValue?>) -> Any? |
| Object  <br> Map             | Map<String,Any?>|
| ArrayBuffer, <br> TypedArray | ArrayBuffer  |
| void                         | Unit  |
| any                          | JSValue |

***注1：Number 类型的转换在已知参数类型的场景下，会自动转成对应的 Int/Long/Double***

***注2：ArrayBuffer 可直接操作 napi 中的指针，但需注意⚠️不要进行释放***

***注3：Function 类型暂时不支持自动推断，只能以 Array<JSValue?> 接收入参***
