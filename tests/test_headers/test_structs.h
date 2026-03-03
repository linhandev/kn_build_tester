#ifndef TEST_STRUCTS_H
#define TEST_STRUCTS_H

#include <stdint.h>

/* --- Size categories --- */

/* small struct (<=16 bytes) */
struct SmallStruct {
    int a;
    int b;
};

/* medium struct (17-64 bytes) */
struct MediumStruct {
    char data[48];
};

/* large struct (>64 bytes) */
struct LargeStruct {
    char data[128];
};

/* --- Opaque (forward declared only) --- */
struct OpaqueRecord;

/* --- Anonymous struct via typedef --- */
typedef struct {
    int x;
    int y;
} Point;

/* --- Truly anonymous struct (as variable type) --- */
struct {
    int anon_a;
    int anon_b;
} anon_struct_instance;

/* --- Bitfield --- */
struct WithBitfield {
    unsigned int flag : 1;
    unsigned int mode : 3;
    unsigned int value : 12;
};

/* --- Nested struct --- */
struct Outer {
    struct Inner {
        int val;
    } inner;
    int extra;
};

/* --- Anonymous nested member --- */
struct WithAnonMember {
    struct {
        int x;
        int y;
    };
    int z;
};

/* --- Function pointer field (vtable-like) --- */
struct VTable {
    int (*open)(void *ctx);
    int (*close)(void *ctx);
    int (*read)(void *ctx, void *buf, int len);
};

/* --- Struct by value field --- */
struct WithStructField {
    struct SmallStruct embedded;
    int extra;
};

/* --- Union field --- */
struct WithUnionField {
    int type;
    union {
        int ival;
        float fval;
    } value;
};

/* --- Nested union with anonymous member --- */
struct WithNestedUnion {
    int kind;
    union {
        int integer;
        double floating;
    };
};

/* --- Self-referential (linked list) --- */
struct ListNode {
    int data;
    struct ListNode *next;
};

/* --- Pragma-packed struct --- */
#pragma pack(push, 1)
struct PragmaPacked {
    char a;
    int b;
};
#pragma pack(pop)

/* --- Attribute packed --- */
struct __attribute__((packed)) AttrPacked {
    char a;
    int b;
};

/* --- Attribute aligned --- */
struct __attribute__((aligned(16))) AttrAligned {
    int value;
};

/* --- Struct return by value (small <=16) --- */
struct SmallStruct ret_small_struct(void);

/* --- Struct return by value (large >16) --- */
struct LargeStruct ret_large_struct(void);

/* --- Struct param by value (small) --- */
void take_small_struct(struct SmallStruct s);

/* --- Struct param by value (large) --- */
void take_large_struct(struct LargeStruct s);

/* --- has_fixed_array_field, has_multidim_array_field --- */
struct WithArrayFields {
    int arr[5];
    int mat[2][3];
};

#endif
