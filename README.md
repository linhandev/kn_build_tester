ets 调用 testNapi.add 触发 so 加载，so中存在对未定义函数 the_missing_symbol 的调用（编译时通过 --unresolved-symbols=ignore-all 绕过了，运行时间崩溃，模拟伙伴场景）

问题：
- faultlog开头是和缺少符号无关的问题
- 真正有用的log在hilog中
  04-03 11:32:50.968  9002  9002 W C03F00/MUSL-LDSO: relocating failed: symbol not found. dso=/data/storage/el1/bundle/libs/arm64/libreloc_demo.so s=the_missing_symbol use_vna_hash=0 van_hash=0
