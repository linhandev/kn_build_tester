#include "napi/native_api.h"

extern "C" void kn_helloworld(void);
extern "C" const char *kn_getTimeZoneRulesViaI18n(void);

static napi_value RunHelloWorld(napi_env env, napi_callback_info info)
{
    kn_helloworld();
    return nullptr;
}

static napi_value Add(napi_env env, napi_callback_info info)
{
    size_t argc = 2;
    napi_value args[2] = {nullptr};

    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);

    napi_valuetype valuetype0;
    napi_typeof(env, args[0], &valuetype0);

    napi_valuetype valuetype1;
    napi_typeof(env, args[1], &valuetype1);

    double value0;
    napi_get_value_double(env, args[0], &value0);

    double value1;
    napi_get_value_double(env, args[1], &value1);

    napi_value sum;
    napi_create_double(env, value0 + value1, &sum);

    return sum;

}

static napi_value GetTimeZoneRulesViaI18n(napi_env env, napi_callback_info info)
{
    const char *msg = kn_getTimeZoneRulesViaI18n();
    napi_value result;
    if (msg == nullptr) {
        napi_get_null(env, &result);
        return result;
    }
    napi_create_string_utf8(env, msg, NAPI_AUTO_LENGTH, &result);
    return result;
}

EXTERN_C_START
static napi_value Init(napi_env env, napi_value exports)
{
    napi_property_descriptor desc[] = {
        { "add", nullptr, Add, nullptr, nullptr, nullptr, napi_default, nullptr },
        { "runHelloWorld", nullptr, RunHelloWorld, nullptr, nullptr, nullptr, napi_default, nullptr },
        { "getTimeZoneRulesViaI18n", nullptr, GetTimeZoneRulesViaI18n, nullptr, nullptr, nullptr, napi_default,
          nullptr },
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
