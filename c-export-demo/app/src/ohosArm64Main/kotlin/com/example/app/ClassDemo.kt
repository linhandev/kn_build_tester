@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.example.app

// ═══════════════════════════════════════════════════════════════════
// Scenario 3: Classes, interfaces, enums, objects, sealed classes
// ═══════════════════════════════════════════════════════════════════

// ── 3A: Open class with constructor, methods, properties ──
// In C: struct with _type(), constructor, method pointers, property getters/setters
// Access: symbols()->kotlin.root.com.example.app.Animal.Animal(name, age)
//         symbols()->kotlin.root.com.example.app.Animal.speak(thiz)
//         symbols()->kotlin.root.com.example.app.Animal.get_name(thiz)
open class Animal(val name: String, var age: Int) {
    open fun speak(): String = "$name says nothing"
    fun description(): String = "$name, age $age"
}

// ── 3B: Subclass ──
// Inherits Animal's struct, overrides speak()
class Dog(name: String, age: Int, val breed: String) : Animal(name, age) {
    override fun speak(): String = "$name barks"
}

// ── 3C: Data class ──
// Exported with constructor, copy(), equals(), hashCode(), toString(), componentN()
data class Point(val x: Double, val y: Double) {
    fun distanceTo(other: Point): Double {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

// ── 3D: Interface ──
// Exported as a struct with _type() and method pointers.
// Cannot be constructed directly — only via implementing class.
interface Greeter {
    fun greet(name: String): String
}

class FriendlyGreeter : Greeter {
    override fun greet(name: String): String = "Hey there, $name! 👋"
}

// ── 3E: Enum ──
// Each entry becomes a nested struct with a get() function.
// Access: symbols()->kotlin.root.com.example.app.Color.RED.get()
enum class Color(val rgb: Int) {
    RED(0xFF0000),
    GREEN(0x00FF00),
    BLUE(0x0000FF);

    fun hexString(): String = "#${rgb.toString(16).padStart(6, '0')}"
}

// ── 3F: Singleton object ──
// Has _instance() instead of a constructor.
// Access: symbols()->kotlin.root.com.example.app.AppConfig._instance()
object AppConfig {
    val maxRetries: Int = 3
    val appName: String = "CExportDemo"
    fun summary(): String = "$appName (max retries: $maxRetries)"
}

// ── 3G: Sealed class ──
// Each subclass is exported separately; the sealed parent has _type() only.
sealed class Shape {
    class Circle(val radius: Double) : Shape()
    class Rectangle(val width: Double, val height: Double) : Shape()
}
