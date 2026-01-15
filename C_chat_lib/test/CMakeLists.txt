# 테스트 프레임워크 라이브러리
add_library(test_framework STATIC
    src/test_framework.c
)

target_include_directories(test_framework PUBLIC
    "${CMAKE_CURRENT_SOURCE_DIR}/include"
)

# 단위 테스트들

# Protocol 테스트
add_executable(protocol_test protocol_test.c)
target_link_libraries(protocol_test PRIVATE
    common
    socket_lib
    test_framework
)

# Command Queue 테스트
add_executable(command_queue_test command_queue_test.c)
target_link_libraries(command_queue_test PRIVATE
    common
    test_framework
    Threads::Threads
)

# Command 테스트
add_executable(command_test command_test.c)
target_link_libraries(command_test PRIVATE
    common
    test_framework
)

# Socket Utils 테스트
add_executable(socket_utils_test socket_utils_test.c)
target_link_libraries(socket_utils_test PRIVATE
    socket_lib
    test_framework
)

# Protocol 경계값 테스트
add_executable(protocol_edge_test protocol_edge_test.c)
target_link_libraries(protocol_edge_test PRIVATE
    common
    socket_lib
    test_framework
)

# Command 경계값 테스트
add_executable(command_edge_test command_edge_test.c)
target_link_libraries(command_edge_test PRIVATE
    common
    test_framework
)

# 통합 테스트
add_executable(echo_test echo_test.c)
target_link_libraries(echo_test PRIVATE
    client_lib
    server_lib
    common
    socket_lib
    Threads::Threads
)

# 클라이언트-서버 통합 테스트 (다중 클라이언트)
add_executable(client_server_integration_test client_server_integration_test.c)
target_link_libraries(client_server_integration_test PRIVATE
    client_lib
    server_lib
    common
    socket_lib
    test_framework
    Threads::Threads
)

# CTest 통합
enable_testing()

# 단위 테스트 등록
add_test(NAME ProtocolTest COMMAND protocol_test)
add_test(NAME SocketUtilsTest COMMAND socket_utils_test)
add_test(NAME CommandQueueTest COMMAND command_queue_test)
add_test(NAME CommandTest COMMAND command_test)

# 경계값 테스트 등록
add_test(NAME ProtocolEdgeTest COMMAND protocol_edge_test)
add_test(NAME CommandEdgeTest COMMAND command_edge_test)

# 통합 테스트 등록
add_test(NAME EchoTest COMMAND echo_test)
add_test(NAME ClientServerIntegrationTest COMMAND client_server_integration_test)

# 테스트 실행 시 출력 포맷 설정
set_tests_properties(ProtocolTest PROPERTIES
    TIMEOUT 30
)

set_tests_properties(SocketUtilsTest PROPERTIES
    TIMEOUT 30
)

set_tests_properties(CommandQueueTest PROPERTIES
    TIMEOUT 30
)

set_tests_properties(CommandTest PROPERTIES
    TIMEOUT 30
)

set_tests_properties(ProtocolEdgeTest PROPERTIES
    TIMEOUT 60
)

set_tests_properties(CommandEdgeTest PROPERTIES
    TIMEOUT 60
)

set_tests_properties(EchoTest PROPERTIES
    TIMEOUT 60
)

set_tests_properties(ClientServerIntegrationTest PROPERTIES
    TIMEOUT 120
)

# 모든 테스트를 실행하는 타겟
add_custom_target(run_all_tests
    COMMAND ${CMAKE_CTEST_COMMAND} --output-on-failure
    DEPENDS protocol_test socket_utils_test command_queue_test command_test 
            protocol_edge_test command_edge_test echo_test client_server_integration_test
    COMMENT "Running all tests..."
)
