#ifndef TEST_PRIMITIVES_H
#define TEST_PRIMITIVES_H

#include <stdbool.h>
#include <stdint.h>
#include <stddef.h>
#include <uchar.h>
#include <wchar.h>

/* --- Functions returning every primitive type --- */
bool          ret_bool(void);
char          ret_char(void);
signed char   ret_schar(void);
unsigned char ret_uchar(void);
short         ret_short(void);
unsigned short ret_ushort(void);
int           ret_int(void);
unsigned int  ret_uint(void);
long          ret_long(void);
unsigned long ret_ulong(void);
long long     ret_longlong(void);
unsigned long long ret_ulonglong(void);
float         ret_float(void);
double        ret_double(void);
long double   ret_longdouble(void);
wchar_t       ret_wchar(void);
char16_t      ret_char16(void);
char32_t      ret_char32(void);
_Complex double ret_complex(void);

/* --- Functions taking every primitive as param --- */
void take_bool(bool b);
void take_char(char c);
void take_schar(signed char c);
void take_uchar(unsigned char c);
void take_short(short s);
void take_ushort(unsigned short s);
void take_int(int i);
void take_uint(unsigned int u);
void take_long(long l);
void take_ulong(unsigned long l);
void take_longlong(long long ll);
void take_ulonglong(unsigned long long ll);
void take_float(float f);
void take_double(double d);
void take_longdouble(long double ld);
void take_wchar(wchar_t w);
void take_char16(char16_t c);
void take_char32(char32_t c);

/* --- void return --- */
void void_func(void);

#endif
