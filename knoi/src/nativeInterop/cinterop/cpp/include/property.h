#include "napi/native_api.h"
#include "js_native_api.h"
#include "js_native_api_types.h"

#ifndef NAPI_BRIDGE_PROPERTY_H
#define NAPI_BRIDGE_PROPERTY_H

#ifdef __cplusplus
extern "C" {
#endif

napi_value getPropertyNames(napi_env env, napi_value value);

napi_value getPropertyValue(napi_env env, napi_value obj, const char *key);

void setPropertyValue(napi_env env, napi_value obj, const char *key, napi_value value);
#ifdef __cplusplus
}
#endif

#endif //NAPI_BRIDGE_PROPERTY_H
