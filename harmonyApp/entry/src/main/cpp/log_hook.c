#include "hilog/log.h"
#include <stddef.h>
#include <stdio.h>

/* Incremented when __wrap_OH_LOG_PrintMsg runs (see -Wl,--wrap=OH_LOG_PrintMsg when linking libentry). */
int g_oh_log_print_hook_calls = 0;

/* Provided by the linker for -Wl,--wrap=OH_LOG_PrintMsg; forwards to libhilog's implementation. */
extern int __real_OH_LOG_PrintMsg(LogType type, LogLevel level, unsigned int domain, const char *tag,
                                  const char *message);

int __wrap_OH_LOG_PrintMsg(LogType type, LogLevel level, unsigned int domain, const char *tag, const char *message)
{
    g_oh_log_print_hook_calls++;
    if (message == NULL) {
        return __real_OH_LOG_PrintMsg(type, level, domain, tag, message);
    }
    char buf[1024];
    const int n = snprintf(buf, sizeof buf, "[wrap #%d] %s", g_oh_log_print_hook_calls, message);
    if (n < 0 || (size_t)n >= sizeof buf) {
        buf[sizeof(buf) - 1] = '\0';
    }
    return __real_OH_LOG_PrintMsg(type, level, domain, tag, buf);
}
