// Test: undefined symbol + -Wl,-z,lazy + --unresolved-symbols=ignore-all.
// If loader respects lazy: exe loads; first call to OH_HiTrace_IsTraceEnabled triggers resolution.
// If not: "Error relocating ... symbol not found" at load (EXIT=127).
#include <iostream>
#include <dlfcn.h>

// extern "C" __attribute__((weak)) bool OH_HiTrace_IsTraceEnabled(void);

int main() {
    std::cout << "main entered (exe loaded)\n";
    void* h = dlopen("libhitrace_ndk.z.so", RTLD_NOW | RTLD_GLOBAL);
    if (!h) {
        std::cout << "dlopen failed: " << dlerror() << "\n";
        return 1;
    }
    std::cout << "dlopen ok, calling OH_HiTrace_IsTraceEnabled...\n";
    bool r = OH_HiTrace_IsTraceEnabled();  // first use -> resolve now
    std::cout << "trace_enabled=" << static_cast<int>(r) << "\n";
    dlclose(h);
    return 0;
}
