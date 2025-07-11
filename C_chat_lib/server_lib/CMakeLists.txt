cmake_minimum_required(VERSION 3.28)
project(server_lib C)

add_library(server_lib SHARED
        src/chat_server.c
)

target_compile_features(server_lib PRIVATE c_std_11)

target_include_directories(server_lib PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_link_libraries(server_lib PUBLIC
        socket_lib
        common
)
