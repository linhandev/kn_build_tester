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
add_library(kn SHARED IMPORTED)
# Resolve ABI-specific prebuilt path by target ABI first (more reliable than host CPU)
set(LIB_ARCH "${CMAKE_OHOS_ARCH_ABI}")
if(NOT LIB_ARCH)
    set(LIB_ARCH "arm64-v8a")
endif()
set_target_properties(kn PROPERTIES IMPORTED_LOCATION
    "${CMAKE_CURRENT_SOURCE_DIR}/../../../libs/${LIB_ARCH}/libkn.so")

target_link_libraries(entry PUBLIC libace_napi.z.so)
target_link_libraries(entry PRIVATE kn)
