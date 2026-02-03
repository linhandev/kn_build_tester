#include <iostream>
#include <hitrace/trace.h>

static bool wrapped_api() {
    return OH_HiTrace_IsTraceEnabled();
}

int main() {
    std::cout << "Before wrapped_api\n";
    bool r = wrapped_api();
    std::cout << "Called wrapped_api, trace_enabled=" << r << "\n";
    return 0;
}
