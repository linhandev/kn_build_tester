- codegen
```ll
  %5 = invoke i32 @_6b6e2d73616d706c652f55736572732f757365722f6769742f73616d706c652f6b6e2d73616d706c652f7372632f6e61746976654d61696e2f6b6f746c696e2f6d61696e2e6b74_knbridge0(i32 42, i32 58)
```

- cstub
```c
extern const int (*_6b6e2d73616d706c652f55736572732f757365722f6769742f73616d706c652f6b6e2d73616d706c652f7372632f6e61746976654d61696e2f6b6f746c696e2f6d61696e2e6b74_target1)(int, int) __asm("knifunptr_hello0_add_numbers");
int _6b6e2d73616d706c652f55736572732f757365722f6769742f73616d706c652f6b6e2d73616d706c652f7372632f6e61746976654d61696e2f6b6f746c696e2f6d61696e2e6b74_knbridge0(int p1, int p2) {
return _6b6e2d73616d706c652f55736572732f757365722f6769742f73616d706c652f6b6e2d73616d706c652f7372632f6e61746976654d61696e2f6b6f746c696e2f6d61696e2e6b74_target1(p1, p2);
}
```

LinkBitcodeDependencies

```
@knifunptr_hello0_add_numbers = local_unnamed_addr global ptr @hello_add_numbers_wrapper0, align 8

; Function Attrs: alwaysinline uwtable
define i32 @hello_add_numbers_wrapper0(i32 noundef %0, i32 noundef %1) #61 {
  %3 = tail call i32 @add_numbers(i32 noundef %0, i32 noundef %1)
  ret i32 %3
}
```

ld.ldd在libhello.so里找到@add_numbers的实现，生成通过got.plt调用函数的汇编

```
First call:  Code → GOT.PLT → PLT stub → Dynamic linker → Resolve → Update GOT.PLT → Call real function
Second call: Code → GOT.PLT → Direct jump to real function in libhello.so
```