# multiple definition demo

This branch demonstrated the multiple definition issue when kn incremental build feature is enabled. kn community's youtrack is tracking this issue with [KT-81760](https://youtrack.jetbrains.com/issue/KT-81760)

To see this issue in action, enable ic for ohos in the kolin version ur using and run

```bash
hdc shell hilog | grep Konan
# in another terminal
bash run.sh
```

hilog would show which f() implementation is selected when ic is disabled.
