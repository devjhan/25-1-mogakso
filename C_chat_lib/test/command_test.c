//
// Command 모듈 단위 테스트
//

#include "test_framework.h"
#include "command.h"
#include "protocol.h"
#include <string.h>

TEST(test_create_send_command) {
    const int client_fd = 123;
    const message_type_t msg_type = MSG_TYPE_CHAT_TEXT;
    const char* payload = "Test message";
    const size_t payload_len = strlen(payload);
    
    command_t* cmd = create_send_command(client_fd, msg_type, (const uint8_t*)payload, payload_len);
    
    ASSERT_NOT_NULL(cmd);
    ASSERT_EQ_INT(cmd->type, CMD_SEND_MESSAGE);
    ASSERT_EQ_INT(cmd->data.send_cmd.target_client_fd, client_fd);
    ASSERT_EQ_INT(cmd->data.send_cmd.msg_type, msg_type);
    ASSERT_EQ_SIZE(cmd->data.send_cmd.payload_len, payload_len);
    ASSERT_NOT_NULL(cmd->data.send_cmd.payload);
    ASSERT_MEMEQ(cmd->data.send_cmd.payload, payload, payload_len);
    
    destroy_command(cmd);
}

TEST(test_create_send_command_empty_payload) {
    const int client_fd = 456;
    const message_type_t msg_type = MSG_TYPE_PING;
    
    command_t* cmd = create_send_command(client_fd, msg_type, NULL, 0);
    
    ASSERT_NOT_NULL(cmd);
    ASSERT_EQ_INT(cmd->type, CMD_SEND_MESSAGE);
    ASSERT_EQ_INT(cmd->data.send_cmd.target_client_fd, client_fd);
    ASSERT_EQ_INT(cmd->data.send_cmd.msg_type, msg_type);
    ASSERT_EQ_SIZE(cmd->data.send_cmd.payload_len, 0U);
    ASSERT_NULL(cmd->data.send_cmd.payload);
    
    destroy_command(cmd);
}

TEST(test_create_broadcast_command) {
    const message_type_t msg_type = MSG_TYPE_USER_JOIN_NOTICE;
    const char* payload = "User joined";
    const size_t payload_len = strlen(payload);
    const int exclude_fd = 789;
    
    command_t* cmd = create_broadcast_command(msg_type, (const uint8_t*)payload, payload_len, exclude_fd);
    
    ASSERT_NOT_NULL(cmd);
    ASSERT_EQ_INT(cmd->type, CMD_BROADCAST_MESSAGE);
    ASSERT_EQ_INT(cmd->data.broadcast_cmd.msg_type, msg_type);
    ASSERT_EQ_SIZE(cmd->data.broadcast_cmd.payload_len, payload_len);
    ASSERT_EQ_INT(cmd->data.broadcast_cmd.exclude_client_fd, exclude_fd);
    ASSERT_NOT_NULL(cmd->data.broadcast_cmd.payload);
    ASSERT_MEMEQ(cmd->data.broadcast_cmd.payload, payload, payload_len);
    
    destroy_command(cmd);
}

TEST(test_create_broadcast_command_empty_payload) {
    const message_type_t msg_type = MSG_TYPE_SERVER_NOTICE;
    const int exclude_fd = -1;
    
    command_t* cmd = create_broadcast_command(msg_type, NULL, 0, exclude_fd);
    
    ASSERT_NOT_NULL(cmd);
    ASSERT_EQ_INT(cmd->type, CMD_BROADCAST_MESSAGE);
    ASSERT_EQ_INT(cmd->data.broadcast_cmd.msg_type, msg_type);
    ASSERT_EQ_SIZE(cmd->data.broadcast_cmd.payload_len, 0U);
    ASSERT_EQ_INT(cmd->data.broadcast_cmd.exclude_client_fd, exclude_fd);
    ASSERT_NULL(cmd->data.broadcast_cmd.payload);
    
    destroy_command(cmd);
}

TEST(test_destroy_command_null) {
    destroy_command(NULL); // NULL 안전하게 처리되어야 함
}

TEST(test_destroy_command_send) {
    const char* payload = "Payload data";
    command_t* cmd = create_send_command(1, MSG_TYPE_CHAT_TEXT, (const uint8_t*)payload, strlen(payload));
    
    ASSERT_NOT_NULL(cmd);
    destroy_command(cmd);
    // 메모리 해제 확인은 valgrind 등 도구로 확인
}

TEST(test_destroy_command_broadcast) {
    const char* payload = "Broadcast data";
    command_t* cmd = create_broadcast_command(MSG_TYPE_USER_LEAVE_NOTICE, (const uint8_t*)payload, strlen(payload), 5);
    
    ASSERT_NOT_NULL(cmd);
    destroy_command(cmd);
}

TEST(test_command_large_payload) {
    const size_t large_size = 1024 * 1024; // 1MB
    uint8_t* large_payload = (uint8_t*)malloc(large_size);
    ASSERT_NOT_NULL(large_payload);
    
    // 페이로드 초기화
    memset(large_payload, 0xAB, large_size);
    
    command_t* cmd = create_send_command(999, MSG_TYPE_FILE_CHUNK, large_payload, large_size);
    
    ASSERT_NOT_NULL(cmd);
    ASSERT_EQ_SIZE(cmd->data.send_cmd.payload_len, large_size);
    ASSERT_MEMEQ(cmd->data.send_cmd.payload, large_payload, large_size);
    
    destroy_command(cmd);
    free(large_payload);
}

// 메인 함수
int main(void) {
    test_init("Command Tests");
    
    RUN_TEST(test_create_send_command);
    RUN_TEST(test_create_send_command_empty_payload);
    RUN_TEST(test_create_broadcast_command);
    RUN_TEST(test_create_broadcast_command_empty_payload);
    RUN_TEST(test_destroy_command_null);
    RUN_TEST(test_destroy_command_send);
    RUN_TEST(test_destroy_command_broadcast);
    RUN_TEST(test_command_large_payload);
    
    test_finish();
    
    return test_get_exit_code();
}
