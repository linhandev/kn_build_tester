//
// Created on 2024/4/11.
//
// Node APIs are not fully supported. To solve the compilation error of the interface cannot be found,
// please include "napi/native_api.h".

#ifndef OHOSAPP_ASYNC_INVOKER_H
#define OHOSAPP_ASYNC_INVOKER_H

#include <js_native_api.h>
EXTERN_C_START

napi_value create_function_waiter(napi_env env, napi_callback_info info);

napi_value notify_function_waiter(napi_env env, napi_callback_info info);

napi_value wait_on_function_waiter(napi_env env, napi_callback_info info);

EXTERN_C_END
#endif //OHOSAPP_ASYNC_INVOKER_H
