
#include "napi/native_api.h"
#include "js_native_api.h"
#include "js_native_api_types.h"

#ifndef NAPI_BRIDGE_TYPE_CONVERTER_H
#define NAPI_BRIDGE_TYPE_CONVERTER_H
#ifdef __cplusplus
extern "C" {
#endif

char *toKString(napi_env env, napi_value value);

int toInt(napi_env env, napi_value value);

long toLong(napi_env env, napi_value value);

napi_value convertStringToNapiValue(napi_env env, const char *value);

napi_value convertIntToNapiValue(napi_env env, int value);

napi_value convertLongToNapiValue(napi_env env, long value);

bool toBoolean(napi_env env, napi_value value);

napi_value convertBooleanToNapiValue(napi_env env, bool value);

double toDouble(napi_env env, napi_value value);

napi_value convertDoubleToNapiValue(napi_env env, double value);

napi_valuetype typeOf(napi_env env, napi_value value);

#ifdef __cplusplus
}
#endif
#endif //NAPI_BRIDGE_TYPE_CONVERTER_H
