#include "napi/native_api.h"
#include "js_native_api.h"
#include "js_native_api_types.h"

#ifdef __cplusplus
extern "C" {
#endif

napi_status
loadModuleWithInfo(napi_env env, const char *path, const char *module_info, napi_value *result);

napi_status loadModule(napi_env env, const char *path, napi_value *result);

#ifdef __cplusplus
}
#endif
