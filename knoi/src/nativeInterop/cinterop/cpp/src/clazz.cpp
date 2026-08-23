#include "clazz.h"

napi_value newInstance(napi_env env, napi_value constructor) {
    napi_value result;
    napi_new_instance(env, constructor, 0, NULL, &result);
    return result;
}