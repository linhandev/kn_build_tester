cmake_minimum_required(VERSION 3.12)
project(entry_module)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

file(GLOB MODULE_SOURCES CONFIGURE_DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/modules/*.cpp")

add_library(entry SHARED
    entry_module.cpp
    ${MODULE_SOURCES}
)

target_include_directories(entry
    PRIVATE
      ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Link prebuilt libkn.so
# 关键: libentry.so 的 UND 符号 kn_run* 由 libkn.so 提供。IMPORTED target 默认不写
# DT_NEEDED, 会导致 libentry.so 的 NEEDED 列表为空 → 运行时 dlopen libentry.so 时 kn_
# 符号无处解析 → NAPI 模块加载失败 → ArkTS `import * as lib from 'libentry.so'` 得到
# undefined → 点"运行验证"报 "Cannot read property runXxxModuleSmokeTest of undefined"。
# 修复: 用 -l:libkn.so 显式让链接器记 DT_NEEDED=libkn.so(libkn.so 自带 SONAME=libkn.so),
# 加 -L 让链接器在 libs 目录找到它。不再依赖 IMPORTED_LOCATION 的隐式符号解析。
add_library(kn SHARED IMPORTED)
set(LIB_ARCH "${CMAKE_OHOS_ARCH_ABI}")
if(NOT LIB_ARCH)
    set(LIB_ARCH "arm64-v8a")
endif()
set(KN_LIB_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../libs/${LIB_ARCH}")
set_target_properties(kn PROPERTIES IMPORTED_LOCATION "${KN_LIB_DIR}/libkn.so")

target_link_libraries(entry PUBLIC libace_napi.z.so)
target_link_libraries(entry PRIVATE kn)
# 显式记 DT_NEEDED=libkn.so, 让运行时 dlopen libentry.so 能按 soname 找到 libkn.so
target_link_options(entry PRIVATE "-L${KN_LIB_DIR}" "-l:libkn.so")
