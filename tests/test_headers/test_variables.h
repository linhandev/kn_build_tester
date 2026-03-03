#ifndef TEST_VARIABLES_H
#define TEST_VARIABLES_H

#include "test_structs.h"

/* --- Storage class / qualifier tags --- */
extern int global_extern_var;
static int global_static_var = 42;
extern const int global_const_var;
extern volatile int global_volatile_var;
extern _Thread_local int global_thread_local_var;

/* --- classify_type tags on variables --- */
typedef int MyVarInt;

extern MyVarInt var_typedef_ref;                /* typedef_ref */
extern enum Color var_enum;                     /* enum (needs Color from test_enums.h) */
extern void *var_void_ptr;                      /* void_ptr */
extern char *var_char_ptr;                      /* char_ptr */
extern const char *var_const_char_ptr;          /* const_char_ptr */
extern struct SmallStruct *var_struct_ptr;      /* struct_ptr */
struct ForwardOnly;
extern struct ForwardOnly *var_opaque_ptr;      /* opaque_ptr */
extern int *var_int_ptr;                        /* ptr */
extern int **var_double_ptr;                    /* double_ptr */
extern void (*var_func_ptr)(int);               /* function_pointer */
extern const int *var_ptr_to_const;             /* ptr_to_const */
extern int *const var_const_ptr;                /* const_ptr */
extern volatile int *var_ptr_to_volatile;       /* ptr_to_volatile */
extern int var_fixed_array[10];                 /* fixed_array */
extern struct SmallStruct var_record;           /* record */

#endif
