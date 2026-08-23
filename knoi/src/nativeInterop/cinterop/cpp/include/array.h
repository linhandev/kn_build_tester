#include <string.h>
#include "napi/native_api.h"
#include "js_native_api.h"
#include "js_native_api_types.h"

#ifndef NAPI_BRIDGE_ARRAY_H
#define NAPI_BRIDGE_ARRAY_H

#ifdef __cplusplus
extern "C" {
#endif

napi_value createArray(napi_env env);

uint32_t getArrayLength(napi_env env, napi_value value);

napi_value getElementInArray(napi_env env, napi_value value, int index);

void setElementInArray(napi_env env, napi_value array, int index, napi_value value);

bool isArray(napi_env env, napi_value value);

#ifdef __cplusplus
}
#endif
#endif //NAPI_BRIDGE_ARRAY_H
