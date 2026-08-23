### Type Export

##### Constant Export

Supports exporting Kotlin constants to JavaScript.

***Note: Due to cross-Runtime calls, the performance overhead is higher compared to same-Runtime constants. For scenarios with large numbers of constants or performance-sensitive cases, it's recommended to define constants directly in ArkTS.***

Use `@KNExport` in Kotlin to export constants as follows:

```kotlin
// Top-level constant
@KNExport
const val testString: String = "testStringDeclare"

// Custom name for the constant in generated ETS code
@KNExport("testStringDeclareCustomName")
const val customName: String = "testStringDeclare"

// Companion object constant in class
class DeclareTest {
    companion object {
        @KNExport
        const val testString: String = "testStringDeclareInClass"
    }
}
```

The generated TypeScript code will be:

```javascript
import { getDeclare } from "@kuiklybase/knoi"

export const testString = getDeclare<string>("com.tencent.tmm.knoi.sample.testString")

export const testStringDeclareCustomName = getDeclare<string>("com.tencent.tmm.knoi.sample.customName")

export const DeclareTest_testString = getDeclare<string>("com.tencent.tmm.knoi.sample.DeclareTest.testString")

```

##### Custom Type Export

Supports exporting Kotlin objects via JSValue.

You can wrap Kotlin objects using `JSValue.wrap` on the Kotlin side:

```kotlin
val rect = Rect(1, 2, 3, 4)
// Wrap rect object
val rectJSValue = JSValue.wrap(rect)
// Define getLeft method with Int return type
rectJSValue["getLeft"] = JSValue.createJSFunction<Int> { jsThis, params ->
    // Unwrap jsThis to get the rect object
    val nativeRect = JSValue.unwrap<Rect>(jsThis)
    // Get first parameter of getLeft method and convert to Int
    val param1 = params[0]!!.toInt()
    return@createJSFunction nativeRect?.l!! + param1
}
```

To use this object in ArkTS, you need to define TypeScript type definitions:
```javascript
export interface Rect {
  getLeft(left: number): number;
}
```