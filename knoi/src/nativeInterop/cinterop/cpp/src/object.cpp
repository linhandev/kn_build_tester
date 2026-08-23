#include <js_native_api.h>
#include "object.h"

napi_value createObject(napi_env env) {
    napi_value obj;
    napi_create_object(env, &obj);
    return obj;
}

napi_value getGlobal(napi_env env) {
    napi_value global;
    napi_get_global(env, &global);
    return global;
}

napi_value getUndefined(napi_env env) {
    napi_value undefined;
    napi_get_undefined(env, &undefined);
    return undefined;
}


void wrap(napi_env env, napi_value js_object, void *finalize_data, void *finalize_cb) {
    napi_wrap(env,
              js_object,
              finalize_data,
              (napi_finalize) finalize_cb,
              nullptr,
              nullptr);
}

void *unwrap(napi_env env, napi_value js_object) {
    void *nativeObj;
    napi_unwrap(env, js_object, &nativeObj);
    return nativeObj;
}

bool isEquals(napi_env env, napi_value a, napi_value b) {
    bool result;
    napi_strict_equals(env, a, b, &result);
    return result;
}

bool isInstanceOf(napi_env env, napi_value constructor, napi_value object) {
    bool result;
    napi_instanceof(env, object, constructor, &result);
    return result;
}