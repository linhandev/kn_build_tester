#ifndef TEST_MACROS_H
#define TEST_MACROS_H

/* object-like macro */
#define MAX_SIZE 1024

/* function-like macro */
#define ADD(a, b) ((a) + (b))

/* empty macro (no body) */
#define EMPTY_MARKER

/* macro using __builtin_ */
#define EXPECT(x) __builtin_expect(!!(x), 1)

/* macro using __typeof */
#define SWAP(a, b) do { __typeof(a) _t = (a); (a) = (b); (b) = _t; } while(0)

/* macro using __extension__ */
#define EXT_CONST __extension__ 0x1234567890ABCDEFull

#endif
