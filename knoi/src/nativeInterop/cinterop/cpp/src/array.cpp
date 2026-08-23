#include "array.h"

napi_value createArray(napi_env env) {
    napi_value array;
    napi_create_array(env, &array);
    return array;
}

uint32_t getArrayLength(napi_env env, napi_value value) {
    uint32_t length;
    napi_get_array_length(env, value, &length);
    return length;
}

napi_value getElementInArray(napi_env env, napi_value value, int index) {
    napi_value result;
    napi_get_element(env, value, index, &result);
    return result;
}

void setElementInArray(napi_env env, napi_value array, int index, napi_value value) {
    napi_set_element(env, array, index, value);
}

bool isArray(napi_env env, napi_value value) {
    bool result = false;
    napi_is_array(env, value, &result);
    return result;
}