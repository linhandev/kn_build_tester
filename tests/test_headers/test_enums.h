#ifndef TEST_ENUMS_H
#define TEST_ENUMS_H

/* Named enum with explicit values */
enum Color {
    COLOR_RED = 0,
    COLOR_GREEN = 1,
    COLOR_BLUE = 2,
};

/* Anonymous enum */
enum {
    ANON_FLAG_A = 10,
    ANON_FLAG_B = 20,
};

/* Enum with negative values */
enum SignedEnum {
    SE_NEG = -100,
    SE_ZERO = 0,
    SE_POS = 100,
};

/* Enum with large range (max - min > 2^31) */
enum LargeRangeEnum {
    LR_MIN = 0,
    LR_MAX = 0x7FFFFFFF,
    LR_HUGE = (long long)0x100000000LL,
};

/* Enum used as bitmask flags */
enum Permissions {
    PERM_READ    = 1,
    PERM_WRITE   = 1 << 1,
    PERM_EXECUTE = 1 << 2,
    PERM_DELETE  = 1 << 3,
    PERM_ALL     = (1 | (1 << 1) | (1 << 2) | (1 << 3)),
};

/* Enum without explicit values */
enum ImplicitEnum {
    IE_FIRST,
    IE_SECOND,
    IE_THIRD,
};

#endif
