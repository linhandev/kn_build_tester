# NApi Bridge

本库旨在解决 Kotlin Native 写 C 逻辑较为晦涩难懂。主要有以下几个职责：

### 职责

1. 承接 Kotlin Native 调用 napi 底层接口的能力
2. 期望使用 C++ 高级特性的代码（但最终暴露仍然需要以 C 符号进行暴露

### 编译指引

1. 创建 build 文件夹

```bash
mkdir build && cd build
```

2. 生成 CMake 所需的编译文件

```bash
cmake -DOHOS_STL=c++_shared -DCMAKE_BUILD_TYPE=Rlease -DOHOS_PLATFORM=OHOS -DCMAKE_TOOLCHAIN_FILE=$OHOS_SDK_NATIVE/build/cmake/ohos.toolchain.cmake ..
```

3. 编译

```bash
cmake --build .
```