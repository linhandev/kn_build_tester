#include "property.h"

napi_value getPropertyNames(napi_env env, napi_value value) {
    napi_value names;
    napi_get_property_names(env, value, &names);
    return names;
}

napi_value getPropertyValue(napi_env env, napi_value obj, const char *key) {
    napi_value result;
    napi_get_named_property(env, obj, key, &result);
    return result;
}

void setPropertyValue(napi_env env, napi_value obj, const char *key, napi_value value) {
    napi_set_named_property(env, obj, key, value);
}