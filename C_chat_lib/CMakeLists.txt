cmake_minimum_required(VERSION 3.28)
project(chat_project C)

#add_subdirectory(socket_lib)
#add_subdirectory(client_lib)
#add_subdirectory(server_lib)
#add_subdirectory(common)

#option(BUILD_TESTS "Build the test executables" ON)

#if(BUILD_TESTS)
    # test 디렉토리도 빌드에 포함
#    add_subdirectory(test)
#endif()

file(GLOB_RECURSE ALL_SOURCES
        "client_lib/src/*.c"
        "server_lib/src/*.c"
        "common/src/*.c"
        "socket_lib/src/*.c"
)
add_library(chat SHARED ${ALL_SOURCES})

target_include_directories(chat PUBLIC
        "${CMAKE_CURRENT_SOURCE_DIR}/client_lib/include"
        "${CMAKE_CURRENT_SOURCE_DIR}/server_lib/include"
        "${CMAKE_CURRENT_SOURCE_DIR}/common/include"
        "${CMAKE_CURRENT_SOURCE_DIR}/socket_lib/include"
)

find_package(Threads REQUIRED)
target_link_libraries(chat PRIVATE Threads::Threads)

