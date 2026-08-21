#include "napi/native_api.h"
#include "libc2k_api.h"

#include <pthread.h>
#include <stdio.h>

extern "C" void kn_eb_ping(void);
extern "C" void kn_eb_alloc(void);
extern "C" void kn_eb_throw(void);

static char g_msg[256];
static int g_fail;
static int g_eb_crossed;

static void fail(const char* what)
{
    if (g_fail) {
        return;
    }
    g_fail = 1;
    snprintf(g_msg, sizeof(g_msg), "FAIL %s", what);
}

static void* pingWorker(void*)
{
    kn_cname_ping();
    kn_eb_ping();
    return nullptr;
}

// Don't catch Kotlin EH on the NAPI/JS thread: unwind leaves x28/thread-state
// unrestored and ArkTS SIGSEGV on return. Catch on a worker instead.
static void* ebThrowWorker(void*)
{
    kn_eb_alloc();
    try {
        kn_eb_throw();
    } catch (...) {
        g_eb_crossed = 1;
    }
    return nullptr;
}

static void runSafeTests()
{
    g_fail = 0;
    g_eb_crossed = 0;
    g_msg[0] = 0;

    kn_cname_ping();
    kn_eb_ping();
    kn_cname_alloc();
    kn_eb_alloc();

    pthread_t t;
    pthread_create(&t, nullptr, pingWorker, nullptr);
    pthread_join(t, nullptr);

    pthread_t th;
    pthread_create(&th, nullptr, ebThrowWorker, nullptr);
    pthread_join(th, nullptr);
    if (!g_eb_crossed) {
        fail("eb throw stayed inside Kotlin");
        return;
    }

    snprintf(g_msg, sizeof(g_msg), "PASS ping alloc pthread ebCrossed");
}

static napi_value RunHelloWorld(napi_env env, napi_callback_info info)
{
    (void)info;
    runSafeTests();
    napi_value result;
    napi_create_string_utf8(env, g_msg, NAPI_AUTO_LENGTH, &result);
    return result;
}

// CAdapter 壳内 terminate，进程死掉就是「异常不出 C 调用点」。
static napi_value RunCnameThrow(napi_env env, napi_callback_info info)
{
    (void)info;
    kn_cname_throw();
    napi_value result;
    napi_get_undefined(env, &result);
    return result;
}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports)
{
    napi_property_descriptor desc[] = {
        { "runHelloWorld", nullptr, RunHelloWorld, nullptr, nullptr, nullptr, napi_default, nullptr },
        { "runCnameThrow", nullptr, RunCnameThrow, nullptr, nullptr, nullptr, napi_default, nullptr },
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
