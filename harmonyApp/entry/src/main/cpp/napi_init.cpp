#include "napi/native_api.h"
#include "libc2k_api.h"

#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

extern "C" int kn_eb_add(int a, int b);
extern "C" int kn_eb_alloc(void);
extern "C" char* kn_eb_greet(const char* name);
extern "C" int kn_eb_caught(void);

static char g_msg[256];
static int g_fail;

static void fail(const char* what)
{
    if (g_fail) {
        return;
    }
    g_fail = 1;
    snprintf(g_msg, sizeof(g_msg), "FAIL %s", what);
}

static void* worker(void*)
{
    if (kn_cname_add(4, 1) != 5) {
        fail("cname pthread add");
    }
    if (kn_eb_add(4, 1) != 5) {
        fail("eb pthread add");
    }
    return nullptr;
}

static void runTests()
{
    g_fail = 0;
    g_msg[0] = 0;

    if (kn_cname_add(2, 3) != 5 || kn_eb_add(2, 3) != 5) {
        fail("add");
        return;
    }
    if (kn_cname_alloc() != 5 || kn_eb_alloc() != 5) {
        fail("alloc/initRuntime");
        return;
    }

    const char* cg = kn_cname_greet("lin");
    char* eg = kn_eb_greet("lin");
    if (cg == nullptr || eg == nullptr || strcmp(cg, "hi lin") != 0 || strcmp(eg, "hi lin") != 0) {
        fail("greet");
        free(eg);
        return;
    }
    libc2k_symbols()->DisposeString(cg);
    free(eg);

    if (kn_cname_caught() != 7 || kn_eb_caught() != 7) {
        fail("caught");
        return;
    }

    pthread_t t;
    pthread_create(&t, nullptr, worker, nullptr);
    pthread_join(t, nullptr);
    if (g_fail) {
        return;
    }

    snprintf(g_msg, sizeof(g_msg), "PASS add alloc greet caught pthread");
}

static napi_value RunHelloWorld(napi_env env, napi_callback_info info)
{
    (void)info;
    runTests();
    napi_value result;
    napi_create_string_utf8(env, g_msg, NAPI_AUTO_LENGTH, &result);
    return result;
}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports)
{
    napi_property_descriptor desc[] = {
        { "runHelloWorld", nullptr, RunHelloWorld, nullptr, nullptr, nullptr, napi_default, nullptr },
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
    .nm_priv = ((void*)0),
    .reserved = { 0 },
};

extern "C" __attribute__((constructor)) void RegisterEntryModule(void)
{
    napi_module_register(&demoModule);
}
