# multiple definition demo

This branch demonstrated the multiple definition issue when kn incremental build feature is enabled. kn community's youtrack is tracking this issue with [KT-81760](https://youtrack.jetbrains.com/issue/KT-81760)

To see this issue in action, enable ic for ohos in the kolin version ur using and run

```bash
hdc shell hilog | grep Konan
# in another terminal
bash run.sh
```

hilog would show which f() implementation is selected when ic is disabled.

in debug build, .symtable contains 4 sumbols from pkg, note there's only one symbol for f()

```
000000000025fd80 t kfun:pkg#f(){}
000000000025fde0 t kfun:pkg#getK(){}kotlin.String
000000000025fda0 t kfun:pkg#getO(){}kotlin.String
000000000025fe20 t kfun:pkg#main(){}
```

Whichever module is specified as dependency in app module's build.gradle.kts first, it's f() implementation will be used during compile.
