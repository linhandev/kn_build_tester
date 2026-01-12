# the minimum version of CMake.
cmake_minimum_required(VERSION 3.5.0)
project(NativeCppDemo)

set(NATIVERENDER_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR})

if(DEFINED PACKAGE_FIND_FILE)
    include(${PACKAGE_FIND_FILE})
endif()

include_directories(${NATIVERENDER_ROOT_PATH}
                    ${NATIVERENDER_ROOT_PATH}/include)

add_library(c2k SHARED IMPORTED)
set_target_properties(c2k PROPERTIES IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../../../libs/arm64-v8a/libc2k.so)

add_library(entry SHARED napi_init.cpp asan.cpp)
target_link_libraries(entry PUBLIC libace_napi.z.so c2k libhilog_ndk.z.so)