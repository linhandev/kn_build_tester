#include "napi/native_api.h"

#include <cstdlib>
#include <string>

extern "C" void AddKitRegistrar(void (*fn)(napi_env, napi_value));

/**
 * Kotlin/Native @CName("kn_runDrawingModuleSmokeTest")，由 libkn.so 提供。
 * 参数为 TTF 在设备上的可读绝对路径（空串则跳过 RegisterFont / RegisterFontBuffer 场景）。
 */
extern "C" char *kn_runDrawingModuleSmokeTest(const char *fontTtfPath);

static std::string GetUtf8String(napi_env env, napi_value val) {
    size_t len = 0;
    napi_get_value_string_utf8(env, val, nullptr, 0, &len);
    std::string s(len, '\0');
    size_t written = 0;
    napi_get_value_string_utf8(env, val, &s[0], len + 1, &written);
    s.resize(written);
    return s;
}

static napi_value RunDrawingModuleSmokeTest(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value args[1];
    napi_get_cb_info(env, info, &argc, args, nullptr, nullptr);
    std::string path;
    if (argc >= 1) {
        path = GetUtf8String(env, args[0]);
    }

    char *raw = kn_runDrawingModuleSmokeTest(path.c_str());
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

static void RegisterDrawingModuleExports(napi_env env, napi_value exports) {
    napi_property_descriptor props[] = {
        {"runDrawingModuleSmokeTest", nullptr, RunDrawingModuleSmokeTest, nullptr, nullptr, nullptr, napi_default, nullptr},
    };
    napi_define_properties(env, exports, sizeof(props) / sizeof(props[0]), props);
}

static void __drawing_module_auto_register() __attribute__((constructor));

static void __drawing_module_auto_register() {
    AddKitRegistrar(RegisterDrawingModuleExports);
}
