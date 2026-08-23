#include "napi/native_api.h"
#include <cstdlib>
#include <dlfcn.h>

typedef void (*RegisterKnSymbolsFuncPtr)(napi_env, napi_value, const char*, const char*);
static RegisterKnSymbolsFuncPtr g_registerKnSymbolsFuncPtr = nullptr;

static const char *BUNDLE_NAME = "com.kotlin.demo";
static const char *MODULE_NAME = "entry";

static void append_kotlin_exports(napi_env env, napi_value exports)
{
    if (g_registerKnSymbolsFuncPtr == nullptr) {
        auto handle = dlopen("libkn.so", RTLD_LAZY);
        if (handle == nullptr) return;
        g_registerKnSymbolsFuncPtr = reinterpret_cast<RegisterKnSymbolsFuncPtr>(
            dlsym(handle, "org_cpf_kotlin_akinterop_register"));
    }
    if (g_registerKnSymbolsFuncPtr == nullptr) return;
    g_registerKnSymbolsFuncPtr(env, exports, BUNDLE_NAME, MODULE_NAME);
}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports)
{
    append_kotlin_exports(env, exports);
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
