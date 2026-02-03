#include <iostream>
#include <dlfcn.h>
#include <stdexcept>

extern "C" __attribute__((weak)) bool OH_HiTrace_IsTraceEnabled(void);

static bool wrapped_api() {
    if (!OH_HiTrace_IsTraceEnabled)
        throw std::runtime_error("OH_HiTrace_IsTraceEnabled not found");
    return OH_HiTrace_IsTraceEnabled();
}

int main() {
    std::cout << "Before wrapped_api\n";
    try {
        // OH_HiTrace_IsTraceEnabled(); // signal 11
        bool r1 = wrapped_api();
        std::cout << "Called wrapped_api before dlopen, trace_enabled=" << r1 << "\n";
    } catch (const std::exception& e) {
        std::cout << "exception (before dlopen): " << e.what() << "\n";
    }
    void* h = dlopen("libhitrace_ndk.z.so", RTLD_GLOBAL);
    try {
        bool r2 = wrapped_api();
        std::cout << "Called wrapped_api after dlopen, trace_enabled=" << r2 << "\n";
        if (h) dlclose(h);
        return 0;
    } catch (const std::exception& e) {
        std::cout << "exception (after dlopen): " << e.what() << "\n";
        if (h) dlclose(h);
        return 1;
    }
}
