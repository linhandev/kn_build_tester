#include "hilog/log.h"
#include <stdio.h>

#define LOG_HOOK_SAMPLE_LINES 5

/* Several PrintMsg calls so HiLog shows multiple wrapped lines (tag LogHookTest). */
void entry_call_oh_log_msg(void)
{
    for (int i = 1; i <= LOG_HOOK_SAMPLE_LINES; i++) {
        char msg[64];
        (void)snprintf(msg, sizeof msg, "entry_caller line %d", i);
        OH_LOG_PrintMsg(LOG_APP, LOG_INFO, 0x0000, "LogHookTest", msg);
    }
}

int entry_log_hook_sample_line_count(void)
{
    return LOG_HOOK_SAMPLE_LINES;
}
