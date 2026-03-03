#ifndef TEST_FUNCTIONS_H
#define TEST_FUNCTIONS_H

#include "test_structs.h"

/* --- variadic function --- */
int my_printf(const char *fmt, ...);

/* --- static inline with body --- */
static inline int add_inline(int a, int b) {
    return a + b;
}

/* --- inline (non-static) with body --- */
inline int mul_inline(int a, int b) {
    return a * b;
}

/* --- function with unnamed parameter --- */
void unnamed_param(int, int count);

/* --- function returning pointer --- */
int* ret_ptr_func(void);

/* --- function with const-qualified return --- */
const int* ret_const_qualified(void);

/* --- function with volatile param --- */
void take_volatile_int(volatile int v);

/* --- return enum type --- */
enum FuncEnum { FE_A, FE_B };
enum FuncEnum ret_enum_func(void);

/* --- return typedef'd type --- */
typedef int StatusCode;
StatusCode ret_typedef_func(void);

/* --- param with typedef'd type --- */
void take_typedef_param(StatusCode code);

/* --- function pointer parameter (callback pattern) --- */
void register_callback(void (*handler)(int event, void *ctx));

/* --- param record (struct) by pointer --- */
void take_record_ptr(struct SmallStruct *s);

/* --- param enum --- */
void take_enum_param(enum FuncEnum e);

#endif
