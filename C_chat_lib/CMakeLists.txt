cmake_minimum_required(VERSION 3.28)
project(chat_project C)

# 빌드 옵션 설정
option(BUILD_TESTS "Build the test executables" ON)

# 라이브러리 서브 디렉토리 추가
add_subdirectory(socket_lib)
add_subdirectory(common)
add_subdirectory(client_lib)
add_subdirectory(server_lib)

# 메인 라이브러리 빌드
file(GLOB_RECURSE ALL_SOURCES
        "client_lib/src/*.c"
        "server_lib/src/*.c"
        "common/src/*.c"
        "socket_lib/src/*.c"
)
add_library(chat SHARED ${ALL_SOURCES}
        common/src/command_queue.c
        common/src/command.c)

target_include_directories(chat PUBLIC
        "${CMAKE_CURRENT_SOURCE_DIR}/client_lib/include"
        "${CMAKE_CURRENT_SOURCE_DIR}/server_lib/include"
        "${CMAKE_CURRENT_SOURCE_DIR}/common/include"
        "${CMAKE_CURRENT_SOURCE_DIR}/socket_lib/include"
)

find_package(Threads REQUIRED)
target_link_libraries(chat PRIVATE Threads::Threads)

# 라이브러리 복사 자동화 (post-build step)
if(APPLE)
    if(CMAKE_SYSTEM_PROCESSOR MATCHES "arm64|aarch64")
        set(PLATFORM_DIR "darwin-aarch64")
    elseif(CMAKE_SYSTEM_PROCESSOR MATCHES "x86_64")
        set(PLATFORM_DIR "darwin-x86_64")
    else()
        set(PLATFORM_DIR "darwin-${CMAKE_SYSTEM_PROCESSOR}")
    endif()
    set(LIB_EXT ".dylib")
elseif(UNIX AND NOT APPLE)
    if(CMAKE_SYSTEM_PROCESSOR MATCHES "x86_64")
        set(PLATFORM_DIR "linux-x86_64")
    elseif(CMAKE_SYSTEM_PROCESSOR MATCHES "aarch64|arm64")
        set(PLATFORM_DIR "linux-aarch64")
    else()
        set(PLATFORM_DIR "linux-${CMAKE_SYSTEM_PROCESSOR}")
    endif()
    set(LIB_EXT ".so")
else()
    set(PLATFORM_DIR "unknown")
    set(LIB_EXT ".dll")
endif()

set(PROJECT_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/..")
set(JAVA_TARGET_DIR "${PROJECT_ROOT}/java_chat_server/src/main/resources/${PLATFORM_DIR}")
set(PYTHON_TARGET_DIR "${PROJECT_ROOT}/py_chat_client/resources/${PLATFORM_DIR}")

# Post-build step: 라이브러리를 Java/Python 프로젝트로 복사
add_custom_command(TARGET chat POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E make_directory "${JAVA_TARGET_DIR}"
    COMMAND ${CMAKE_COMMAND} -E make_directory "${PYTHON_TARGET_DIR}"
    COMMAND ${CMAKE_COMMAND} -E copy
        $<TARGET_FILE:chat>
        "${JAVA_TARGET_DIR}/libchat${LIB_EXT}"
    COMMAND ${CMAKE_COMMAND} -E copy
        $<TARGET_FILE:chat>
        "${PYTHON_TARGET_DIR}/libchat${LIB_EXT}"
    COMMENT "Copying library to Java and Python projects..."
)

# 커스텀 타겟: 라이브러리만 복사 (빌드 없이)
add_custom_target(copy_library
    COMMAND ${CMAKE_COMMAND} -E make_directory "${JAVA_TARGET_DIR}"
    COMMAND ${CMAKE_COMMAND} -E make_directory "${PYTHON_TARGET_DIR}"
    COMMAND ${CMAKE_COMMAND} -E copy
        $<TARGET_FILE:chat>
        "${JAVA_TARGET_DIR}/libchat${LIB_EXT}"
    COMMAND ${CMAKE_COMMAND} -E copy
        $<TARGET_FILE:chat>
        "${PYTHON_TARGET_DIR}/libchat${LIB_EXT}"
    COMMENT "Copying library to Java and Python projects..."
    DEPENDS chat
)

# 테스트 빌드 (옵션으로 제어)
if(BUILD_TESTS)
    enable_testing()
    add_subdirectory(test)
endif()
