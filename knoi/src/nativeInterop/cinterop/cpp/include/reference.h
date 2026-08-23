#ifndef NAPI_BRIDGE_REFERENCE_H
#define NAPI_BRIDGE_REFERENCE_H

#include <js_native_api.h>

#ifdef __cplusplus
extern "C" {
#endif
/***
 * 创建引用，防止被回收，不再使用时请调用 deleteReference
 */

napi_ref createReference(napi_env env, napi_value value);

void deleteReference(napi_env env, napi_ref ref);

napi_value getReferenceValue(napi_env env, napi_ref ref);

#endif //NAPI_BRIDGE_REFERENCE_H

#ifdef __cplusplus
}
#endif