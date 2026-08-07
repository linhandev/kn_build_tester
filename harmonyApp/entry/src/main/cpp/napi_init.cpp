#include "napi/native_api.h"

#include <dlfcn.h>
#include <hilog/log.h>
#include <string>

#undef LOG_DOMAIN
#undef LOG_TAG
#define LOG_DOMAIN 0x0A00
#define LOG_TAG "ondemand"

using RunFn = const char *(*)();

static std::string g_lastError;

static void *OpenSo(const char *soName)
{
    dlerror();
    void *handle = dlopen(soName, RTLD_NOW);
    if (handle != nullptr) {
        return handle;
    }
    std::string path = std::string("/data/storage/el1/bundle/libs/arm64/") + soName;
    dlerror();
    return dlopen(path.c_str(), RTLD_NOW);
}

static const char *CallExported(const char *soName, const char *symName)
{
    g_lastError.clear();
    void *handle = OpenSo(soName);
    if (handle == nullptr) {
        const char *err = dlerror();
        g_lastError = std::string("dlopen ") + soName + " failed: " + (err ? err : "?");
        OH_LOG_ERROR(LOG_APP, "%{public}s", g_lastError.c_str());
        return nullptr;
    }
    dlerror();
    auto *fn = reinterpret_cast<RunFn>(dlsym(handle, symName));
    const char *err = dlerror();
    if (fn == nullptr || err != nullptr) {
        g_lastError = std::string("dlsym ") + symName + " failed: " + (err ? err : "null");
        OH_LOG_ERROR(LOG_APP, "%{public}s", g_lastError.c_str());
        return nullptr;
    }
    const char *msg = fn();
    if (msg == nullptr) {
        g_lastError = std::string(symName) + " returned null";
        OH_LOG_ERROR(LOG_APP, "%{public}s", g_lastError.c_str());
        return nullptr;
    }
    OH_LOG_INFO(LOG_APP, "called %{public}s!%{public}s -> %{public}s", soName, symName, msg);
    return msg;
}

static napi_value MakeString(napi_env env, const char *msg)
{
    napi_value result;
    if (msg == nullptr) {
        const char *fallback = g_lastError.empty() ? "(null)" : g_lastError.c_str();
        napi_create_string_utf8(env, fallback, NAPI_AUTO_LENGTH, &result);
        return result;
    }
    napi_create_string_utf8(env, msg, NAPI_AUTO_LENGTH, &result);
    return result;
}

static napi_value LoadFirstModule(napi_env env, napi_callback_info info)
{
    (void)info;
    return MakeString(env, CallExported("libk2n.so", "kn_k2n_run"));
}

static napi_value LoadSecondModule(napi_env env, napi_callback_info info)
{
    (void)info;
    return MakeString(env, CallExported("libn2k.so", "kn_n2k_run"));
}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports)
{
    napi_property_descriptor desc[] = {
        {"loadFirstModule", nullptr, LoadFirstModule, nullptr, nullptr, nullptr, napi_default, nullptr},
        {"loadSecondModule", nullptr, LoadSecondModule, nullptr, nullptr, nullptr, napi_default, nullptr},
    };
    napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
    return exports;
}
EXTERN_C_END

static napi_module demoModule = {
    .nm_version = 1,
    .nm_flags = 0,
    .nm_filename = nullptr,
    .nm_register_func = Init,
    .nm_modname = "entry",
    .nm_priv = ((void *)0),
    .reserved = {0},
};

extern "C" __attribute__((constructor)) void RegisterEntryModule(void)
{
    napi_module_register(&demoModule);
}
