/*
 * Minimal declarations mirroring OpenHarmony i18n/timezone.h layout for cinterop.
 * Used with dlopen("libohi18n*.so") so the app does not link LocalizationKit at load time.
 */
#ifndef I18N_TZ_SHIM_H
#define I18N_TZ_SHIM_H

#include <stddef.h>
#include <stdint.h>

typedef enum DateRuleType {
    DOM = 0,
    DOW = 1,
    DOW_GEQ_DOM = 2,
    DOW_LEQ_DOM = 3
} DateRuleType;

typedef enum TimeRuleType {
    WALL_TIME = 0,
    STANDARD_TIME = 1,
    UTC_TIME = 2
} TimeRuleType;

typedef struct DateTimeRule {
    int32_t month;
    int32_t dayOfMonth;
    int32_t dayOfWeek;
    int32_t weekInMonth;
    int32_t millisInDay;
    DateRuleType dateRuleType;
    TimeRuleType timeRuleType;
} DateTimeRule;

typedef struct InitialTimeZoneRule {
    int32_t rawOffset;
    int32_t dstSavings;
} InitialTimeZoneRule;

typedef struct TimeArrayTimeZoneRule {
    char *name;
    int32_t rawOffset;
    int32_t dstSavings;
    double *startTimes;
    int32_t numStartTimes;
    TimeRuleType timeRuleType;
} TimeArrayTimeZoneRule;

typedef struct AnnualTimeZoneRule {
    char *name;
    int32_t startYear;
    int32_t endYear;
    int32_t rawOffset;
    int32_t dstSavings;
    DateTimeRule dateTimeRule;
} AnnualTimeZoneRule;

typedef struct TimeZoneRules {
    InitialTimeZoneRule initial;
    TimeArrayTimeZoneRule *timeArrayRules;
    AnnualTimeZoneRule *annualRules;
    size_t numTimeArrayRules;
    size_t numAnnualRules;
} TimeZoneRules;

typedef int32_t I18n_ErrorCode;

I18n_ErrorCode OH_i18n_GetTimeZoneRules(const char *timeZoneID, TimeZoneRules *rules);

#endif /* I18N_TZ_SHIM_H */
