#ifndef KONAN_LIBC2K_H
#define KONAN_LIBC2K_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            libc2k_KBoolean;
#else
typedef _Bool           libc2k_KBoolean;
#endif
typedef unsigned short     libc2k_KChar;
typedef signed char        libc2k_KByte;
typedef short              libc2k_KShort;
typedef int                libc2k_KInt;
typedef long long          libc2k_KLong;
typedef unsigned char      libc2k_KUByte;
typedef unsigned short     libc2k_KUShort;
typedef unsigned int       libc2k_KUInt;
typedef unsigned long long libc2k_KULong;
typedef float              libc2k_KFloat;
typedef double             libc2k_KDouble;
typedef float __attribute__ ((__vector_size__ (16))) libc2k_KVector128;
typedef void*              libc2k_KNativePtr;
struct libc2k_KType;
typedef struct libc2k_KType libc2k_KType;

typedef struct {
  libc2k_KNativePtr pinned;
} libc2k_kref_kotlin_Byte;
typedef struct {
  libc2k_KNativePtr pinned;
} libc2k_kref_kotlin_Short;
typedef struct {
  libc2k_KNativePtr pinned;
} libc2k_kref_kotlin_Int;
typedef struct {
  libc2k_KNativePtr pinned;
} libc2k_kref_kotlin_Long;
typedef struct {
  libc2k_KNativePtr pinned;
} libc2k_kref_kotlin_Float;
typedef struct {
  libc2k_KNativePtr pinned;
} libc2k_kref_kotlin_Double;
typedef struct {
  libc2k_KNativePtr pinned;
} libc2k_kref_kotlin_Char;
typedef struct {
  libc2k_KNativePtr pinned;
} libc2k_kref_kotlin_Boolean;
typedef struct {
  libc2k_KNativePtr pinned;
} libc2k_kref_kotlin_Unit;
typedef struct {
  libc2k_KNativePtr pinned;
} libc2k_kref_kotlin_UByte;
typedef struct {
  libc2k_KNativePtr pinned;
} libc2k_kref_kotlin_UShort;
typedef struct {
  libc2k_KNativePtr pinned;
} libc2k_kref_kotlin_UInt;
typedef struct {
  libc2k_KNativePtr pinned;
} libc2k_kref_kotlin_ULong;

extern void kn_helloworld();

typedef struct {
  /* Service functions. */
  void (*DisposeStablePointer)(libc2k_KNativePtr ptr);
  void (*DisposeString)(const char* string);
  libc2k_KBoolean (*IsInstance)(libc2k_KNativePtr ref, const libc2k_KType* type);
  libc2k_kref_kotlin_Byte (*createNullableByte)(libc2k_KByte);
  libc2k_KByte (*getNonNullValueOfByte)(libc2k_kref_kotlin_Byte);
  libc2k_kref_kotlin_Short (*createNullableShort)(libc2k_KShort);
  libc2k_KShort (*getNonNullValueOfShort)(libc2k_kref_kotlin_Short);
  libc2k_kref_kotlin_Int (*createNullableInt)(libc2k_KInt);
  libc2k_KInt (*getNonNullValueOfInt)(libc2k_kref_kotlin_Int);
  libc2k_kref_kotlin_Long (*createNullableLong)(libc2k_KLong);
  libc2k_KLong (*getNonNullValueOfLong)(libc2k_kref_kotlin_Long);
  libc2k_kref_kotlin_Float (*createNullableFloat)(libc2k_KFloat);
  libc2k_KFloat (*getNonNullValueOfFloat)(libc2k_kref_kotlin_Float);
  libc2k_kref_kotlin_Double (*createNullableDouble)(libc2k_KDouble);
  libc2k_KDouble (*getNonNullValueOfDouble)(libc2k_kref_kotlin_Double);
  libc2k_kref_kotlin_Char (*createNullableChar)(libc2k_KChar);
  libc2k_KChar (*getNonNullValueOfChar)(libc2k_kref_kotlin_Char);
  libc2k_kref_kotlin_Boolean (*createNullableBoolean)(libc2k_KBoolean);
  libc2k_KBoolean (*getNonNullValueOfBoolean)(libc2k_kref_kotlin_Boolean);
  libc2k_kref_kotlin_Unit (*createNullableUnit)(void);
  libc2k_kref_kotlin_UByte (*createNullableUByte)(libc2k_KUByte);
  libc2k_KUByte (*getNonNullValueOfUByte)(libc2k_kref_kotlin_UByte);
  libc2k_kref_kotlin_UShort (*createNullableUShort)(libc2k_KUShort);
  libc2k_KUShort (*getNonNullValueOfUShort)(libc2k_kref_kotlin_UShort);
  libc2k_kref_kotlin_UInt (*createNullableUInt)(libc2k_KUInt);
  libc2k_KUInt (*getNonNullValueOfUInt)(libc2k_kref_kotlin_UInt);
  libc2k_kref_kotlin_ULong (*createNullableULong)(libc2k_KULong);
  libc2k_KULong (*getNonNullValueOfULong)(libc2k_kref_kotlin_ULong);

  /* User functions. */
  struct {
    struct {
      void (*helloworld)();
    } root;
  } kotlin;
} libc2k_ExportedSymbols;
extern libc2k_ExportedSymbols* libc2k_symbols(void);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_LIBC2K_H */
