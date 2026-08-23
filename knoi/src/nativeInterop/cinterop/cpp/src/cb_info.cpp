#include <cstddef>
#include <malloc.h>
#include "cb_info.h"

int getCallbackInfoParamsSize(napi_env env, napi_callback_info info) {
    size_t argc;
    napi_get_cb_info(env, info, &argc, NULL, NULL, NULL);
    return argc;
}

void* getCbInfoData(napi_env env, napi_callback_info info) {
    void *data = NULL;
    napi_get_cb_info(env, info, 0, NULL, NULL, &data);
    return data;
}

napi_value *getCbInfoWithSize(napi_env env, napi_callback_info info, int size) {
    size_t argc = (size_t) size;
    napi_value *args = (napi_value *) malloc((size) * sizeof(napi_value));
    napi_get_cb_info(env, info, &argc, args, NULL, NULL);
    return args;
}

napi_value getCbInfoThis(napi_env env, napi_callback_info info) {
    napi_value jsThis;
    napi_get_cb_info(env, info, nullptr, nullptr, &jsThis, nullptr);
    return jsThis;
}