
#include "reference.h"

/***
 * 创建引用，防止被回收，不再使用时请调用 deleteReference
 */
napi_ref createReference(napi_env env, napi_value value) {
    napi_ref ref;
    napi_create_reference(env, value, 1, &ref);
    return ref;
}

void deleteReference(napi_env env, napi_ref ref) {
    napi_delete_reference(env, ref);
}

napi_value getReferenceValue(napi_env env, napi_ref ref) {
    napi_value value;
    napi_get_reference_value(env, ref, &value);
    return value;
}