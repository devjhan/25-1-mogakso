# CMake generated Testfile for 
# Source directory: /Users/jhan_acc/projects/chat_project/C_chat_lib/test
# Build directory: /Users/jhan_acc/projects/chat_project/C_chat_lib/build/test
# 
# This file includes the relevant testing commands required for 
# testing this directory and lists subdirectories to be tested as well.
add_test([=[ProtocolTest]=] "/Users/jhan_acc/projects/chat_project/C_chat_lib/build/test/protocol_test")
set_tests_properties([=[ProtocolTest]=] PROPERTIES  TIMEOUT "30" _BACKTRACE_TRIPLES "/Users/jhan_acc/projects/chat_project/C_chat_lib/test/CMakeLists.txt;82;add_test;/Users/jhan_acc/projects/chat_project/C_chat_lib/test/CMakeLists.txt;0;")
add_test([=[SocketUtilsTest]=] "/Users/jhan_acc/projects/chat_project/C_chat_lib/build/test/socket_utils_test")
set_tests_properties([=[SocketUtilsTest]=] PROPERTIES  TIMEOUT "30" _BACKTRACE_TRIPLES "/Users/jhan_acc/projects/chat_project/C_chat_lib/test/CMakeLists.txt;83;add_test;/Users/jhan_acc/projects/chat_project/C_chat_lib/test/CMakeLists.txt;0;")
add_test([=[CommandQueueTest]=] "/Users/jhan_acc/projects/chat_project/C_chat_lib/build/test/command_queue_test")
set_tests_properties([=[CommandQueueTest]=] PROPERTIES  TIMEOUT "30" _BACKTRACE_TRIPLES "/Users/jhan_acc/projects/chat_project/C_chat_lib/test/CMakeLists.txt;84;add_test;/Users/jhan_acc/projects/chat_project/C_chat_lib/test/CMakeLists.txt;0;")
add_test([=[CommandTest]=] "/Users/jhan_acc/projects/chat_project/C_chat_lib/build/test/command_test")
set_tests_properties([=[CommandTest]=] PROPERTIES  TIMEOUT "30" _BACKTRACE_TRIPLES "/Users/jhan_acc/projects/chat_project/C_chat_lib/test/CMakeLists.txt;85;add_test;/Users/jhan_acc/projects/chat_project/C_chat_lib/test/CMakeLists.txt;0;")
add_test([=[ProtocolEdgeTest]=] "/Users/jhan_acc/projects/chat_project/C_chat_lib/build/test/protocol_edge_test")
set_tests_properties([=[ProtocolEdgeTest]=] PROPERTIES  TIMEOUT "60" _BACKTRACE_TRIPLES "/Users/jhan_acc/projects/chat_project/C_chat_lib/test/CMakeLists.txt;88;add_test;/Users/jhan_acc/projects/chat_project/C_chat_lib/test/CMakeLists.txt;0;")
add_test([=[CommandEdgeTest]=] "/Users/jhan_acc/projects/chat_project/C_chat_lib/build/test/command_edge_test")
set_tests_properties([=[CommandEdgeTest]=] PROPERTIES  TIMEOUT "60" _BACKTRACE_TRIPLES "/Users/jhan_acc/projects/chat_project/C_chat_lib/test/CMakeLists.txt;89;add_test;/Users/jhan_acc/projects/chat_project/C_chat_lib/test/CMakeLists.txt;0;")
add_test([=[EchoTest]=] "/Users/jhan_acc/projects/chat_project/C_chat_lib/build/test/echo_test")
set_tests_properties([=[EchoTest]=] PROPERTIES  TIMEOUT "60" _BACKTRACE_TRIPLES "/Users/jhan_acc/projects/chat_project/C_chat_lib/test/CMakeLists.txt;92;add_test;/Users/jhan_acc/projects/chat_project/C_chat_lib/test/CMakeLists.txt;0;")
add_test([=[ClientServerIntegrationTest]=] "/Users/jhan_acc/projects/chat_project/C_chat_lib/build/test/client_server_integration_test")
set_tests_properties([=[ClientServerIntegrationTest]=] PROPERTIES  TIMEOUT "120" _BACKTRACE_TRIPLES "/Users/jhan_acc/projects/chat_project/C_chat_lib/test/CMakeLists.txt;93;add_test;/Users/jhan_acc/projects/chat_project/C_chat_lib/test/CMakeLists.txt;0;")
