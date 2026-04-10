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

# Static so -Wl,--wrap applies at link time of libentry.so (not reliable across separate .so).
add_library(loghook STATIC log_hook.c)

add_library(entry SHARED napi_init.cpp log_caller.c)
target_link_options(entry PRIVATE "-Wl,--wrap=OH_LOG_Print")
target_link_libraries(entry PRIVATE loghook)
target_link_libraries(entry PUBLIC libace_napi.z.so c2k hilog_ndk.z)