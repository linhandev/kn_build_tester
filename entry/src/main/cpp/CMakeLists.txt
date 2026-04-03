# the minimum version of CMake.
cmake_minimum_required(VERSION 3.5.0)
project(symbol_not_found)

set(NATIVERENDER_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR})

if(DEFINED PACKAGE_FIND_FILE)
    include(${PACKAGE_FIND_FILE})
endif()

include_directories(${NATIVERENDER_ROOT_PATH})

add_library(reloc_demo SHARED reloc_demo.cpp)
target_link_options(reloc_demo PRIVATE "-Wl,--unresolved-symbols=ignore-all" "-Wl,-z,now")

add_library(entry SHARED napi_init.cpp)
target_link_libraries(entry PUBLIC libace_napi.z.so PRIVATE reloc_demo)