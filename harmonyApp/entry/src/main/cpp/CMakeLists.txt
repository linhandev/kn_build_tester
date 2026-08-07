# the minimum version of CMake.
cmake_minimum_required(VERSION 3.5.0)
project(KotlinDemo)

set(NATIVERENDER_ROOT_PATH ${CMAKE_CURRENT_SOURCE_DIR})
set(KN_LIBS_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../libs/arm64-v8a)

if(DEFINED PACKAGE_FIND_FILE)
    include(${PACKAGE_FIND_FILE})
endif()

include_directories(${NATIVERENDER_ROOT_PATH}
                    ${NATIVERENDER_ROOT_PATH}/include)

# Always link runtime + std. Do NOT link libk2n.so / libn2k.so here —
# they are packaged in libs/ and loaded via dlopen for on-demand.
# libmainstub.so is NEEDED by business SOs (k2n/n2k), not by entry —
# linking it into entry bakes a host absolute path into DT_NEEDED.
add_library(runtime SHARED IMPORTED)
set_target_properties(runtime PROPERTIES IMPORTED_LOCATION ${KN_LIBS_DIR}/libruntime.so)

add_library(std SHARED IMPORTED)
set_target_properties(std PROPERTIES IMPORTED_LOCATION ${KN_LIBS_DIR}/libstd.so)

add_library(entry SHARED napi_init.cpp)
target_link_libraries(entry PUBLIC
    libace_napi.z.so
    libhilog_ndk.z.so
    runtime
    std
)
