#include <iostream>
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
        bool r = wrapped_api();
        std::cout << "Called wrapped_api, trace_enabled=" << static_cast<int>(r) << "\n";
        return 0;
    } catch (const std::exception& e) {
        std::cout << "exception: " << e.what() << "\n";
        return 1;
    }
}
