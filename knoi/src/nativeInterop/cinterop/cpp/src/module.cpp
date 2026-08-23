#include "module.h"

napi_status
loadModuleWithInfo(napi_env env, const char *path, const char *module_info, napi_value *result) {
    return napi_load_module_with_info(env, path, module_info, result);
}

napi_status loadModule(napi_env env, const char *path, napi_value *result) {
    return napi_load_module(env, path, result);
}
