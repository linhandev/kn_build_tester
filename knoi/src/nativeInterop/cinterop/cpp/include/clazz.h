#ifndef NAPI_BRIDGE_CLAZZ_H
#define NAPI_BRIDGE_CLAZZ_H

#include "napi/native_api.h"
#include "js_native_api.h"
#include "js_native_api_types.h"

#ifdef __cplusplus
extern "C" {
#endif
napi_value newInstance(napi_env env, napi_value constructor);
#ifdef __cplusplus
}
#endif
#endif //NAPI_BRIDGE_CLAZZ_H
