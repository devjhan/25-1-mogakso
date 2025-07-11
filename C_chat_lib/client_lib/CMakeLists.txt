cmake_minimum_required(VERSION 3.28)
project(client_lib C)

add_library(client_lib SHARED
        src/chat_client.c
)

target_compile_features(client_lib PRIVATE c_std_11)

target_include_directories(client_lib PUBLIC
        ${CMAKE_CURRENT_SOURCE_DIR}/include
)

target_link_libraries(client_lib PUBLIC
        socket_lib
        common
)
