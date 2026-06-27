#include "napi/native_api.h"

#include <cstdlib>
#include <string>

extern "C" void AddKitRegistrar(void (*fn)(napi_env, napi_value));

extern "C" char *kn_runNetConnectionModuleSmokeTest(const char *dbDir, const char *bundleName, const char *moduleName);

static std::string GetUtf8String(napi_env env, napi_value val) {
    size_t len = 0;
    napi_get_value_string_utf8(env, val, nullptr, 0, &len);
    std::string s(len, '\0');
    size_t written = 0;
    napi_get_value_string_utf8(env, val, &s[0], len + 1, &written);
    s.resize(written);
    return s;
}

static napi_value RunNetConnectionModuleSmokeTest(napi_env env, napi_callback_info info) {
    size_t argc = 3;
    napi_value args[3];
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    if (argc < 3) {
        napi_throw_error(env, nullptr, "需要 3 个参数: databaseDir, bundleName, moduleName");
        return nullptr;
    }

    std::string dbDir = GetUtf8String(env, args[0]);
    std::string bundle = GetUtf8String(env, args[1]);
    std::string module = GetUtf8String(env, args[2]);

    char *raw = kn_runNetConnectionModuleSmokeTest(dbDir.c_str(), bundle.c_str(), module.c_str());
    if (raw == nullptr) {
        napi_value nullVal;
        napi_get_null(env, &nullVal);
        return nullVal;
    }

    napi_value result;
    napi_create_string_utf8(env, raw, NAPI_AUTO_LENGTH, &result);
    std::free(raw);
    return result;
}

static void RegisterNetConnectionModuleExports(napi_env env, napi_value exports) {
    napi_property_descriptor props[] = {
        {"runNetConnectionModuleSmokeTest", nullptr, RunNetConnectionModuleSmokeTest, nullptr, nullptr, nullptr,
         napi_default, nullptr},
    };
    napi_define_properties(env, exports, sizeof(props) / sizeof(props[0]), props);
}

static void __net_connection_module_auto_register() __attribute__((constructor));

static void __net_connection_module_auto_register() {
    AddKitRegistrar(RegisterNetConnectionModuleExports);
}
