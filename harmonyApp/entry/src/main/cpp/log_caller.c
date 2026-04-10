#include "hilog/log.h"

/* Called from entry NAPI: exercises the same C API used with -lhilog_ndk.z. */
void entry_call_oh_log_print(void)
{
    OH_LOG_Print(LOG_APP, LOG_INFO, 0x0000, "LogHookTest", "%{public}s", "entry_caller");
}
