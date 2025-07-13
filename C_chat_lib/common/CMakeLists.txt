cmake_minimum_required(VERSION 3.28)
project(common C)

add_library(common SHARED
        src/protocol.c
        src/command_queue.c
        src/command.c
)

target_compile_features(common PRIVATE c_std_11)

target_include_directories(common PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)