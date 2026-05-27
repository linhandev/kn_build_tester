// ═══════════════════════════════════════════════════════════════════
// C caller demonstrating all Kotlin/Native C export scenarios
// ═══════════════════════════════════════════════════════════════════
//
// Build:
//   clang -I<path-to-header> -L<path-to-so> -lcexport_demo main.c -o demo
//
// The generated header is: libcexport_demo_api.h
// The prefix is: cexport_demo  (derived from baseName with - and . replaced by _)

#include <stdio.h>
#include "libcexport_demo_api.h"

// ── Scenario 7: @ExportedBridge symbols are NOT in the header ──
// Must declare prototypes manually.
extern int kn_raw_add(int a, int b);
extern const char* kn_raw_greeting(void);
extern int kn_raw_strlen(const char* str);

// ── Scenario 2A: @CName(externName) standalone functions ──
// These ARE in the header as extern "C" declarations, but we can also
// call them directly without the struct.
extern int kn_subtract(int a, int b);
extern double kn_divide(double a, double b);
extern const char* kn_hello(void);

int main(void) {
    // Get the exported symbols struct
    cexport_demo_ExportedSymbols* lib = cexport_demo_symbols();

    // ─────────────────────────────────────────────────────────────
    // Scenario 1: Default-named top-level functions
    // ─────────────────────────────────────────────────────────────
    printf("=== Scenario 1: Default naming ===\n");
    int sum = lib->kotlin.root.com.example.app.addNumbers(10, 32);
    printf("addNumbers(10, 32) = %d\n", sum);

    const char* greeting = lib->kotlin.root.com.example.app.greetUser("OHOS");
    printf("greetUser(\"OHOS\") = %s\n", greeting);
    lib->kotlin.root.DisposeString(greeting);

    const char* version = lib->kotlin.root.com.example.app.get_appVersion();
    printf("appVersion = %s\n", version);
    lib->kotlin.root.DisposeString(version);

    // ─────────────────────────────────────────────────────────────
    // Scenario 2A: @CName(externName) — direct extern "C" calls
    // ─────────────────────────────────────────────────────────────
    printf("\n=== Scenario 2A: @CName externName ===\n");
    printf("kn_subtract(100, 58) = %d\n", kn_subtract(100, 58));
    printf("kn_divide(22.0, 7.0) = %f\n", kn_divide(22.0, 7.0));
    printf("kn_hello() = %s\n", kn_hello());

    // ─────────────────────────────────────────────────────────────
    // Scenario 2B: @CName(shortName) — custom struct member name
    // ─────────────────────────────────────────────────────────────
    printf("\n=== Scenario 2B: @CName shortName ===\n");
    int product = lib->kotlin.root.com.example.app.myMultiply(6, 7);
    printf("myMultiply(6, 7) = %d\n", product);

    // ─────────────────────────────────────────────────────────────
    // Scenario 2C: @CName(both) — struct member access
    // ─────────────────────────────────────────────────────────────
    printf("\n=== Scenario 2C: @CName both ===\n");
    double quotient = lib->kotlin.root.com.example.app.div(355.0, 113.0);
    printf("div(355, 113) = %f\n", quotient);

    // ─────────────────────────────────────────────────────────────
    // Scenario 3A: Class — constructor, methods, properties
    // ─────────────────────────────────────────────────────────────
    printf("\n=== Scenario 3A: Class ===\n");
    cexport_demo_kref_com_example_app_Animal animal =
        lib->kotlin.root.com.example.app.Animal.Animal("Cat", 5);
    const char* animalSpeak = lib->kotlin.root.com.example.app.Animal.speak(animal);
    printf("Animal.speak() = %s\n", animalSpeak);
    lib->kotlin.root.DisposeString(animalSpeak);

    const char* animalName = lib->kotlin.root.com.example.app.Animal.get_name(animal);
    printf("Animal.name = %s\n", animalName);
    lib->kotlin.root.DisposeString(animalName);

    int animalAge = lib->kotlin.root.com.example.app.Animal.get_age(animal);
    printf("Animal.age = %d\n", animalAge);

    // ─────────────────────────────────────────────────────────────
    // Scenario 3C: Data class
    // ─────────────────────────────────────────────────────────────
    printf("\n=== Scenario 3C: Data class ===\n");
    cexport_demo_kref_com_example_app_Point p1 =
        lib->kotlin.root.com.example.app.Point.Point(1.0, 2.0);
    cexport_demo_kref_com_example_app_Point p2 =
        lib->kotlin.root.com.example.app.Point.Point(4.0, 6.0);
    double dist = lib->kotlin.root.com.example.app.Point.distanceTo(p1, p2);
    printf("distance(Point(1,2), Point(4,6)) = %f\n", dist);

    // ─────────────────────────────────────────────────────────────
    // Scenario 3E: Enum
    // ─────────────────────────────────────────────────────────────
    printf("\n=== Scenario 3E: Enum ===\n");
    cexport_demo_kref_com_example_app_Color red =
        lib->kotlin.root.com.example.app.Color.RED.get();
    int rgb = lib->kotlin.root.com.example.app.Color.get_rgb(red);
    printf("Color.RED.rgb = 0x%06X\n", rgb);

    // ─────────────────────────────────────────────────────────────
    // Scenario 3F: Singleton object
    // ─────────────────────────────────────────────────────────────
    printf("\n=== Scenario 3F: Singleton object ===\n");
    cexport_demo_kref_com_example_app_AppConfig config =
        lib->kotlin.root.com.example.app.AppConfig._instance();
    const char* summary = lib->kotlin.root.com.example.app.AppConfig.summary(config);
    printf("AppConfig.summary() = %s\n", summary);
    lib->kotlin.root.DisposeString(summary);

    int maxRetries = lib->kotlin.root.com.example.app.AppConfig.get_maxRetries(config);
    printf("AppConfig.maxRetries = %d\n", maxRetries);

    // ─────────────────────────────────────────────────────────────
    // Scenario 4: Visibility — only public symbols exist in header
    // ─────────────────────────────────────────────────────────────
    printf("\n=== Scenario 4: Visibility ===\n");
    const char* pub = lib->kotlin.root.com.example.app.publicTopLevel();
    printf("publicTopLevel() = %s\n", pub);
    lib->kotlin.root.DisposeString(pub);
    // internalTopLevel() and privateTopLevel() do NOT exist in the struct.
    // Attempting to call them would be a compile error.

    // ─────────────────────────────────────────────────────────────
    // Scenario 5: Re-exported lib module API
    // ─────────────────────────────────────────────────────────────
    printf("\n=== Scenario 5: Re-exported lib API ===\n");
    int libSum = lib->kotlin.root.com.example.lib.libAdd(100, 200);
    printf("libAdd(100, 200) = %d\n", libSum);

    const char* libGreeting = lib->kotlin.root.com.example.lib.libGreet("World");
    printf("libGreet(\"World\") = %s\n", libGreeting);
    lib->kotlin.root.DisposeString(libGreeting);

    // ─────────────────────────────────────────────────────────────
    // Scenario 7: @ExportedBridge — raw symbols (no header)
    // ─────────────────────────────────────────────────────────────
    printf("\n=== Scenario 7: @ExportedBridge ===\n");
    printf("kn_raw_add(111, 222) = %d\n", kn_raw_add(111, 222));
    printf("kn_raw_greeting() = %s\n", kn_raw_greeting());
    printf("kn_raw_strlen(\"hello\") = %d\n", kn_raw_strlen("hello"));

    // ─────────────────────────────────────────────────────────────
    // Cleanup: release stable pointers
    // ─────────────────────────────────────────────────────────────
    lib->kotlin.root.DisposeStablePointer(animal.pinned);
    lib->kotlin.root.DisposeStablePointer(p1.pinned);
    lib->kotlin.root.DisposeStablePointer(p2.pinned);
    lib->kotlin.root.DisposeStablePointer(red.pinned);
    lib->kotlin.root.DisposeStablePointer(config.pinned);

    printf("\n✅ All scenarios completed.\n");
    return 0;
}
