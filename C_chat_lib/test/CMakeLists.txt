add_executable(echo_test echo_test.c)

target_link_libraries(echo_test PRIVATE
        client_lib
        server_lib
        common
        pthread
)