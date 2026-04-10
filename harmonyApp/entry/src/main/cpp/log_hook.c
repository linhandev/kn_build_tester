#include "hilog/log.h"
#include <stdarg.h>

/* Incremented when __wrap_OH_LOG_Print runs (see -Wl,--wrap=OH_LOG_Print when linking libentry). */
int g_oh_log_print_hook_calls = 0;

int __wrap_OH_LOG_Print(LogType type, LogLevel level, unsigned int domain, const char *tag, const char *fmt, ...)
{
    g_oh_log_print_hook_calls++;
    va_list ap;
    va_start(ap, fmt);
    const int rc = OH_LOG_VPrint(type, level, domain, tag, fmt, ap);
    va_end(ap);
    return rc;
}
