// ─────────────────────────────────────────────────────────────
// class_usage_demo.c — How to use the C struct generated from a Kotlin class
// ─────────────────────────────────────────────────────────────
//
// Kotlin source:
//
//   open class Animal(val name: String, var age: Int) {
//       open fun speak(): String = "$name says nothing"
//       fun description(): String = "$name, age $age"
//   }
//
//   object AppConfig {
//       val maxRetries: Int = 3
//       val appName: String = "CExportDemo"
//       fun summary(): String = "$appName (max retries: $maxRetries)"
//   }
//
//   enum class Color(val rgb: Int) {
//       RED(0xFF0000), GREEN(0x00FF00), BLUE(0x0000FF)
//   }
//
// Generated header: libcexport_demo_api.h
// Prefix: cexport_demo

#include <stdio.h>
#include <string.h>
#include "libcexport_demo_api.h"

// ═══════════════════════════════════════════════════════════
// Helper: duplicate a C string (the generated header returns
// const char* that must be freed via DisposeString, so we
// strdup if we want to keep it past the DisposeString call)
// ═══════════════════════════════════════════════════════════
static const char* keep(const char* s) {
    char* dup = strdup(s);
    return dup;
}

int main(void) {
    // ── Step 1: Get the symbols struct ──
    // Every access goes through this single function pointer table.
    cexport_demo_ExportedSymbols* lib = cexport_demo_symbols();

    // ═══════════════════════════════════════════════════════════
    // Part A: Class — constructor, methods, val/var, cleanup
    // ═══════════════════════════════════════════════════════════
    printf("=== Animal class ===\n\n");

    // ── A1: Construct an object ──
    // The constructor takes the same args as the Kotlin primary constructor.
    // Returns a kref (stable pointer) — you own this reference.
    cexport_demo_kref_com_example_app_Animal cat =
        lib->kotlin.root.com.example.app.Animal.Animal("Cat", 5);

    // ── A2: Call methods — first arg is always `thiz` (the object) ──
    const char* speech = lib->kotlin.root.com.example.app.Animal.speak(cat);
    printf("speak():    %s\n", speech);
    lib->kotlin.root.DisposeString(speech);  // MUST free returned strings

    const char* desc = lib->kotlin.root.com.example.app.Animal.description(cat);
    printf("description(): %s\n", desc);
    lib->kotlin.root.DisposeString(desc);

    // ── A3: Read a `val` property — getter with thiz ──
    const char* name = lib->kotlin.root.com.example.app.Animal.get_name(cat);
    printf("name (val): %s\n", name);
    lib->kotlin.root.DisposeString(name);

    // ── A4: Read a `var` property — same getter pattern ──
    int age = lib->kotlin.root.com.example.app.Animal.get_age(cat);
    printf("age (var):  %d\n", age);

    // ── A5: Write a `var` property — setter takes (thiz, newValue) ──
    lib->kotlin.root.com.example.app.Animal.set_age(cat, 6);
    age = lib->kotlin.root.com.example.app.Animal.get_age(cat);
    printf("age after set_age(6): %d\n", age);

    // ── A6: Create a second object — each kref is independent ──
    cexport_demo_kref_com_example_app_Animal dog =
        lib->kotlin.root.com.example.app.Animal.Animal("Dog", 3);

    const char* dogSpeech = lib->kotlin.root.com.example.app.Animal.speak(dog);
    printf("\nDog speak(): %s\n", dogSpeech);
    lib->kotlin.root.DisposeString(dogSpeech);

    // ── A7: Dispose ALL krefs — memory leak if you forget! ──
    // DisposeStablePointer releases the Kotlin object reference.
    // The kref struct has a `.pinned` field of type KNativePtr.
    lib->kotlin.root.DisposeStablePointer(cat.pinned);
    lib->kotlin.root.DisposeStablePointer(dog.pinned);
    // After this, cat/dog are INVALID — do not use them again.

    // ═══════════════════════════════════════════════════════════
    // Part B: Singleton object — _instance() instead of constructor
    // ═══════════════════════════════════════════════════════════
    printf("\n=== AppConfig object ===\n\n");

    // Objects use _instance() — always returns the same Kotlin object.
    cexport_demo_kref_com_example_app_AppConfig config =
        lib->kotlin.root.com.example.app.AppConfig._instance();

    // Read properties — note: get_* takes the object as thiz
    int retries = lib->kotlin.root.com.example.app.AppConfig.get_maxRetries(config);
    printf("maxRetries: %d\n", retries);

    const char* appName = lib->kotlin.root.com.example.app.AppConfig.get_appName(config);
    printf("appName:    %s\n", appName);
    lib->kotlin.root.DisposeString(appName);

    const char* summary = lib->kotlin.root.com.example.app.AppConfig.summary(config);
    printf("summary():  %s\n", summary);
    lib->kotlin.root.DisposeString(summary);

    lib->kotlin.root.DisposeStablePointer(config.pinned);

    // ═══════════════════════════════════════════════════════════
    // Part C: Enum — each entry is a nested struct with get()
    // ═══════════════════════════════════════════════════════════
    printf("\n=== Color enum ===\n\n");

    // Each enum entry is accessed as Color.ENTRY.get() → returns a kref
    cexport_demo_kref_com_example_app_Color red =
        lib->kotlin.root.com.example.app.Color.RED.get();

    cexport_demo_kref_com_example_app_Color green =
        lib->kotlin.root.com.example.app.Color.GREEN.get();

    cexport_demo_kref_com_example_app_Color blue =
        lib->kotlin.root.com.example.app.Color.BLUE.get();

    // Read the `rgb` property on each entry
    printf("RED.rgb   = 0x%06X\n", lib->kotlin.root.com.example.app.Color.get_rgb(red));
    printf("GREEN.rgb = 0x%06X\n", lib->kotlin.root.com.example.app.Color.get_rgb(green));
    printf("BLUE.rgb  = 0x%06X\n", lib->kotlin.root.com.example.app.Color.get_rgb(blue));

    // Dispose all enum entry krefs
    lib->kotlin.root.DisposeStablePointer(red.pinned);
    lib->kotlin.root.DisposeStablePointer(green.pinned);
    lib->kotlin.root.DisposeStablePointer(blue.pinned);

    // ═══════════════════════════════════════════════════════════
    // Part D: IsInstance — type checking at runtime
    // ═══════════════════════════════════════════════════════════
    printf("\n=== IsInstance check ===\n\n");

    cexport_demo_kref_com_example_app_Animal animal =
        lib->kotlin.root.com.example.app.Animal.Animal("Fish", 2);

    // _type() returns a KType* for the class
    cexport_demo_KType* animalType =
        lib->kotlin.root.com.example.app.Animal._type();

    // IsInstance(kref, type) → bool
    int isAnimal = lib->kotlin.root.IsInstance(animal.pinned, animalType);
    printf("animal IsInstance(Animal): %s\n", isAnimal ? "true" : "false");

    lib->kotlin.root.DisposeStablePointer(animal.pinned);

    printf("\n✅ Done.\n");
    return 0;
}
