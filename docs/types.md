### Type Conversion

***All method calls and service calls only support the types listed below***

Since Kotlin Native and ArkTs types cannot be perfectly matched, type conversion occurs during cross-Runtime calls with the following rules:

| **JavaScript**               | **Kotlin** |
|------------------------------| ------------------------ |
| Boolean                      | Boolean
| Number                       | Double, Int, Long |
| String                       | String               |
| Array                        | Array, List           |
| Function                     | (args: Array<JSValue?>) -> Any? |
| Object  <br> Map             | Map<String,Any?>|
| ArrayBuffer, <br> TypedArray | ArrayBuffer  |
| void                         | Unit  |
| any                          | JSValue |

***Note 1: Number type conversion will automatically convert to corresponding Int/Long/Double when parameter type is known***

***Note 2: ArrayBuffer can directly manipulate napi pointers, but be careful⚠️ not to release them***

***Note 3: Function types currently don't support automatic inference, can only receive parameters as Array<JSValue?>***