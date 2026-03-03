#ifndef TEST_POINTERS_H
#define TEST_POINTERS_H

struct DefinedStruct { int x; };
struct OpaqueStruct;

/* void pointer */
void* ret_void_ptr(void);
void take_void_ptr(void *p);

/* char pointer */
char* ret_char_ptr(void);
void take_char_ptr(char *s);

/* const char pointer */
const char* ret_const_char_ptr(void);
void take_const_char_ptr(const char *s);

/* double pointer (pointer to pointer) */
int** ret_double_ptr(void);
void take_double_ptr(int **pp);

/* function pointer as parameter */
typedef void (*Callback)(int);
void take_func_ptr(void (*cb)(int, int));
Callback ret_func_ptr(void);

/* struct pointer (defined struct) */
struct DefinedStruct* ret_struct_ptr(void);
void take_struct_ptr(struct DefinedStruct *s);

/* opaque pointer (forward-declared struct) */
struct OpaqueStruct* ret_opaque_ptr(void);
void take_opaque_ptr(struct OpaqueStruct *s);

/* generic typed pointer */
int* ret_int_ptr(void);
void take_int_ptr(int *p);

/* pointer to const (const int*) */
void take_ptr_to_const(const int *p);

/* const pointer (int *const) */
void take_const_ptr(int *const p);

/* pointer to volatile (volatile int*) */
void take_ptr_to_volatile(volatile int *p);

#endif
