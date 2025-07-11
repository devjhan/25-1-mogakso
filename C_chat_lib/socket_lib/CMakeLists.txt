cmake_minimum_required(VERSION 3.28)
project(socket_lib C)

add_library(socket_lib SHARED
        src/socket_utils.c
)

target_compile_features(socket_lib PRIVATE c_std_11)

target_include_directories(socket_lib PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)