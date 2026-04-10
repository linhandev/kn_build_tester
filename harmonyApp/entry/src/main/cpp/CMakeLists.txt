# the minimum version of CMake.
cmake_minimum_required(VERSION 3.5.0)
project(KotlinDemo)

set(NATIVERENDER_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR})

if(DEFINED PACKAGE_FIND_FILE)
    include(${PACKAGE_FIND_FILE})
endif()

include_directories(${NATIVERENDER_ROOT_PATH}
                    ${NATIVERENDER_ROOT_PATH}/include)

add_library(c2k SHARED IMPORTED)
set_target_properties(c2k PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../../../libs/arm64-v8a/libc2k.so)

# Bridge (__wrap_*) and call sites live in the same libentry.so link (no separate .a).
# Same idea if you compile the bridge to bitcode and link that .o/.bc into this shared lib.
add_library(entry SHARED napi_init.cpp log_caller.c log_hook.c)
target_link_options(entry PRIVATE "-Wl,--wrap=OH_LOG_PrintMsg")
target_link_libraries(entry PUBLIC libace_napi.z.so c2k hilog_ndk.z)