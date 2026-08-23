#include "napi/native_api.h"
#include "js_native_api.h"
#include "js_native_api_types.h"

#ifndef NAPI_BRIDGE_CB_INFO_H
#define NAPI_BRIDGE_CB_INFO_H

#ifdef __cplusplus
extern "C" {
#endif

int getCallbackInfoParamsSize(napi_env env, napi_callback_info info);

void *getCbInfoData(napi_env env, napi_callback_info info);

napi_value *getCbInfoWithSize(napi_env env, napi_callback_info info, int size);

napi_value getCbInfoThis(napi_env env, napi_callback_info info);

#ifdef __cplusplus
}
#endif
#endif //NAPI_BRIDGE_CB_INFO_H