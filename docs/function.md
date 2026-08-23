### Function Calling

> For supported parameter and return value types and conversion rules, see the "Type Conversion" section

##### Kotlin Calling ArkTS

- 1、Register JS Method (ArkTS)

```javascript
import { bind } from "@kuiklybase/knoi"

bind("testStringReturnStringForKMM", (name: string) => {
      console.log("sample testStringReturnStringForKMM")
      return name + "forArkTS"
})

// Remember to unbind when no longer in use to prevent memory leaks
unBind("testStringReturnStringForKMM")
```

- 2、KMM Calling ArkTS (Kotlin)

```Kotlin
val strResult = getJSFunction("testStringReturnStringForKMM")?.invoke<String>("KMM")
```

##### ArkTS Calling KMM

- 1、Register KMM Method (Kotlin)

```Kotlin

// Business logic
@KNExport
fun testStringFunction(name: String): String {
    return name + "forKMM"
}

```

- 2、ArkTS Calling KMM (ArkTS)

```javascript
import { invoke } from "@kuiklybase/knoi"

let result: String = invoke<String>("testStringReturnString", "input")
console.log("invoke testStringReturnString result " + result);
```