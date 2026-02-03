#include <iostream>
#include <dlfcn.h>
#include <stdexcept>

// Weak fallback so BIND_NOW load succeeds when lib lacks symbol; call path is via dlsym only.
extern "C" __attribute__((weak)) bool OH_HiTrace_IsTraceEnabled(void) {
    throw std::runtime_error("OH_HiTrace_IsTraceEnabled not found");
}

static bool wrapped_api() {
    void* h = dlopen("libhitrace_ndk.z.so", RTLD_NOW);
    if (!h)
        throw std::runtime_error("dlopen libhitrace_ndk.z.so failed");
    typedef bool (*fn_t)(void);
    fn_t fn = reinterpret_cast<fn_t>(dlsym(h, "OH_HiTrace_IsTraceEnabled"));
    if (!fn) {
        dlclose(h);
        throw std::runtime_error("OH_HiTrace_IsTraceEnabled not found");
    }
    bool r = fn();
    dlclose(h);
    return r;
}

int main() {
    std::cout << "Before wrapped_api\n";
    try {
        bool r = wrapped_api();
        std::cout << "Called wrapped_api, trace_enabled=" << static_cast<int>(r) << "\n";
        return 0;
    } catch (const std::exception& e) {
        std::cout << "exception: " << e.what() << "\n";
        return 1;
    }
}
