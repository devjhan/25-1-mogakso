//
// Command 모듈 경계값 및 에러 처리 테스트
//

#include "test_framework.h"
#include "command.h"
#include "protocol.h"
#include <string.h>
#include <limits.h>

// 경계값 테스트
TEST(test_create_send_command_max_client_fd) {
    const int max_fd = INT_MAX;
    const char* payload = "Test";
    const size_t payload_len = strlen(payload);
    
    command_t* cmd = create_send_command(max_fd, MSG_TYPE_CHAT_TEXT, (const uint8_t*)payload, payload_len);
    
    ASSERT_NOT_NULL(cmd);
    ASSERT_EQ_INT(cmd->data.send_cmd.target_client_fd, max_fd);
    
    destroy_command(cmd);
}

TEST(test_create_send_command_zero_fd) {
    const char* payload = "Test";
    command_t* cmd = create_send_command(0, MSG_TYPE_CHAT_TEXT, (const uint8_t*)payload, strlen(payload));
    
    ASSERT_NOT_NULL(cmd);
    ASSERT_EQ_INT(cmd->data.send_cmd.target_client_fd, 0);
    
    destroy_command(cmd);
}

TEST(test_create_broadcast_command_negative_exclude) {
    const char* payload = "Broadcast";
    const int exclude_fd = -1;  // 브로드캐스트에서 제외하지 않음
    
    command_t* cmd = create_broadcast_command(MSG_TYPE_USER_JOIN_NOTICE, (const uint8_t*)payload, strlen(payload), exclude_fd);
    
    ASSERT_NOT_NULL(cmd);
    ASSERT_EQ_INT(cmd->data.broadcast_cmd.exclude_client_fd, exclude_fd);
    
    destroy_command(cmd);
}

TEST(test_create_command_large_payload) {
    const size_t large_size = 10 * 1024;  // 10KB
    uint8_t* large_payload = (uint8_t*)malloc(large_size);
    ASSERT_NOT_NULL(large_payload);
    memset(large_payload, 0xFF, large_size);
    
    command_t* cmd = create_send_command(123, MSG_TYPE_FILE_CHUNK, large_payload, large_size);
    
    ASSERT_NOT_NULL(cmd);
    ASSERT_EQ_SIZE(cmd->data.send_cmd.payload_len, large_size);
    ASSERT_MEMEQ(cmd->data.send_cmd.payload, large_payload, large_size);
    
    destroy_command(cmd);
    free(large_payload);
}

TEST(test_create_command_all_message_types) {
    // 모든 메시지 타입에 대해 명령 생성 가능한지 테스트
    message_type_t types[] = {
        MSG_TYPE_CHAT_TEXT,
        MSG_TYPE_FILE_INFO,
        MSG_TYPE_FILE_CHUNK,
        MSG_TYPE_FILE_END,
        MSG_TYPE_USER_LOGIN_REQUEST,
        MSG_TYPE_USER_LOGIN_RESPONSE,
        MSG_TYPE_USER_JOIN_NOTICE,
        MSG_TYPE_USER_LEAVE_NOTICE,
        MSG_TYPE_SERVER_NOTICE,
        MSG_TYPE_ERROR_RESPONSE,
        MSG_TYPE_PING,
        MSG_TYPE_PONG
    };
    
    const char* payload = "Test";
    const size_t payload_len = strlen(payload);
    
    for (size_t i = 0; i < sizeof(types) / sizeof(types[0]); i++) {
        command_t* cmd = create_send_command(1, types[i], (const uint8_t*)payload, payload_len);
        ASSERT_NOT_NULL(cmd);
        ASSERT_EQ_INT(cmd->data.send_cmd.msg_type, types[i]);
        destroy_command(cmd);
    }
}

// NULL 및 에러 처리 테스트
TEST(test_destroy_command_null_safety) {
    destroy_command(NULL);  // NULL 안전하게 처리되어야 함
}

TEST(test_create_send_command_null_payload_with_length) {
    // NULL payload지만 길이가 있는 경우
    command_t* cmd = create_send_command(1, MSG_TYPE_CHAT_TEXT, NULL, 100);
    // 현재 구현에서는 NULL payload는 무시하고 길이만 저장할 수 있음
    // 또는 NULL이면 길이 0으로 처리될 수 있음
    if (cmd != NULL) {
        // 구현에 따라 다르지만, 일반적으로 길이 0으로 처리됨
        ASSERT_TRUE(cmd->data.send_cmd.payload_len == 0 || cmd->data.send_cmd.payload_len == 100);
        destroy_command(cmd);
    }
}

TEST(test_create_multiple_commands) {
    // 여러 명령을 생성하고 파괴하는 테스트
    command_t* commands[100];
    
    for (int i = 0; i < 100; i++) {
        char payload[32];
        snprintf(payload, sizeof(payload), "Command %d", i);
        commands[i] = create_send_command(i, MSG_TYPE_CHAT_TEXT, (const uint8_t*)payload, strlen(payload));
        ASSERT_NOT_NULL(commands[i]);
    }
    
    // 모두 파괴
    for (int i = 0; i < 100; i++) {
        destroy_command(commands[i]);
    }
}

// 메모리 할당 실패 시뮬레이션은 어렵지만, 경계값 테스트는 가능
TEST(test_command_payload_exact_size) {
    const size_t payload_size = 1024;
    uint8_t* payload = (uint8_t*)malloc(payload_size);
    ASSERT_NOT_NULL(payload);
    memset(payload, 0xAA, payload_size);
    
    command_t* cmd = create_broadcast_command(MSG_TYPE_FILE_CHUNK, payload, payload_size, -1);
    ASSERT_NOT_NULL(cmd);
    ASSERT_EQ_SIZE(cmd->data.broadcast_cmd.payload_len, payload_size);
    ASSERT_MEMEQ(cmd->data.broadcast_cmd.payload, payload, payload_size);
    
    destroy_command(cmd);
    free(payload);
}

// 메인 함수
int main(void) {
    test_init("Command Edge Cases Tests");
    
    RUN_TEST(test_create_send_command_max_client_fd);
    RUN_TEST(test_create_send_command_zero_fd);
    RUN_TEST(test_create_broadcast_command_negative_exclude);
    RUN_TEST(test_create_command_large_payload);
    RUN_TEST(test_create_command_all_message_types);
    RUN_TEST(test_destroy_command_null_safety);
    RUN_TEST(test_create_send_command_null_payload_with_length);
    RUN_TEST(test_create_multiple_commands);
    RUN_TEST(test_command_payload_exact_size);
    
    test_finish();
    
    return test_get_exit_code();
}
