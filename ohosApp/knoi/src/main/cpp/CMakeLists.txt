# the minimum version of CMake.
cmake_minimum_required(VERSION 3.4.1)
project(KNOI)
add_definitions(-DOHOS_PLATFORM)

set(KNOI_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR})

include_directories(${KNOI_ROOT_PATH}
                    ${KNOI_ROOT_PATH}/include)

add_library(knoi SHARED knoi.cpp async_invoker.cpp)

find_library(
    # Sets the name of the path variable.
    hilog-lib
    # Specifies the name of the NDK library that
    # you want CMake to locate.
    hilog_ndk.z
)

target_link_libraries(knoi PUBLIC libace_napi.z.so libhilog_ndk.z.so)