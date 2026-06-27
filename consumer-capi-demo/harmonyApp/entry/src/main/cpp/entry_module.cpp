#include "napi/native_api.h"

#include <vector>

typedef void (*KitRegistrar)(napi_env, napi_value);

static std::vector<KitRegistrar> &GetKitRegistrars() {
    static std::vector<KitRegistrar> registrars;
    return registrars;
}

extern "C" void AddKitRegistrar(KitRegistrar fn) {
    GetKitRegistrars().push_back(fn);
}

static napi_value Init(napi_env env, napi_value exports) {
    for (auto fn : GetKitRegistrars()) {
        fn(env, exports);
    }
    return exports;
}

NAPI_MODULE(libentry, Init)
