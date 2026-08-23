#ifndef NAPI_BRIDGE_OBJECT_H
#define NAPI_BRIDGE_OBJECT_H

#include <js_native_api_types.h>

#ifdef __cplusplus
extern "C" {
#endif

napi_value createObject(napi_env env);

napi_value getGlobal(napi_env env);

napi_value getUndefined(napi_env env);

void wrap(napi_env env, napi_value js_object, void *data, void *finalize_cb);

void *unwrap(napi_env env, napi_value js_object);

bool isEquals(napi_env env, napi_value a, napi_value b);

bool isInstanceOf(napi_env env, napi_value constructor, napi_value object);

#ifdef __cplusplus
}
#endif
#endif //NAPI_BRIDGE_OBJECT_H
