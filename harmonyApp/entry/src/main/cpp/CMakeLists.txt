# the minimum version of CMake.
cmake_minimum_required(VERSION 3.5.0)
project(harmonyApp)

set(NATIVERENDER_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR})

if(DEFINED PACKAGE_FIND_FILE)
    include(${PACKAGE_FIND_FILE})
endif()

include_directories(${NATIVERENDER_ROOT_PATH}
                    ${NATIVERENDER_ROOT_PATH}/include)

add_library(entry SHARED napi_init.cpp san_test.cpp)
# target_link_options(entry PRIVATE "-Wl,-z,now")
target_link_libraries(entry PUBLIC ${NATIVERENDER_ROOT_PATH}/../../../libs/arm64-v8a/libc2k.so)
target_link_libraries(entry PUBLIC libace_napi.z.so libhilog_ndk.z.so)
