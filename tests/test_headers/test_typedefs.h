#ifndef TEST_TYPEDEFS_H
#define TEST_TYPEDEFS_H

#include <stdint.h>

/* --- typedef to primitive --- */
typedef int MyInt;
typedef wchar_t MyWChar;
typedef char16_t MyChar16;
typedef char32_t MyChar32;

/* --- typedef to struct --- */
struct RealStruct { int x; };
typedef struct RealStruct RealStructAlias;

/* --- typedef to union --- */
union RealUnion { int a; float b; };
typedef union RealUnion RealUnionAlias;

/* --- typedef to enum --- */
enum RealEnum { RE_A, RE_B };
typedef enum RealEnum RealEnumAlias;

/* --- typedef to opaque (forward-declared) --- */
struct OpaqueForTypedef;
typedef struct OpaqueForTypedef OpaqueAlias;

/* --- typedef to pointer --- */
typedef int* IntPtr;

/* --- typedef to function pointer --- */
typedef int (*FuncPtrType)(int a, int b);

/* --- typedef to function type (not pointer) --- */
typedef int FuncType(int a, int b);

/* --- typedef chain (typedef of typedef) --- */
typedef MyInt MyIntChain;

/* --- typedef to array --- */
typedef int IntArray10[10];

/* --- typedef to void --- */
typedef void VoidType;

/* --- C++ type alias (using) --- */
using CppAlias = int;

#endif
