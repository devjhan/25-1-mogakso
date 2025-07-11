//
// Created by jhan_macbook on 25. 6. 30.
//

// test/echo_test.c (main 함수 부분)

#include <stdio.h>
#include <string.h>
#include <assert.h>
#include <unistd.h>
#include "chat_server.h"
#include "chat_client.h"
#include "protocol.h"

static int g_test_passed = 0;
const char* TEST_MESSAGE = "Hello, Echo Test!";

// 서버 측 콜백: 완성된 프로토콜 메시지 수신
void server_on_complete_message(void* user_data, const client_info_t* client, message_type_t msg_type, const uint8_t* payload, size_t len) {
    if (msg_type == MSG_TYPE_CHAT_TEXT) {
        printf("[Server] Received from client %d: %.*s\n", client->socket_fd, (int)len, (const char*)payload);
        printf("[Server] Echoing back to client %d...\n", client->socket_fd);
        // 그대로 에코
        server_send_payload_to_client(client->socket_fd, MSG_TYPE_CHAT_TEXT, payload, len);
    }
}

// 서버 연결/해제 콜백
void server_on_connect(void* user_data, const client_info_t* client) {
    printf("[Server] Client %s (fd: %d) connected.\n", client->ip_addr, client->socket_fd);
}
void server_on_disconnect(void* user_data, const client_info_t* client) {
    printf("[Server] Client %d disconnected.\n", client->socket_fd);
}

// 클라이언트 측 콜백: 완성된 메시지 수신
void client_on_complete_message(void* user_data, message_type_t msg_type, const uint8_t* payload, size_t len) {
    if (msg_type == MSG_TYPE_CHAT_TEXT) {
        printf("[Client] Received from server: %.*s\n", (int)len, (const char*)payload);
        // 안전하게 비교 (payload는 널 종료 문자열이 아님)
        if (len == strlen(TEST_MESSAGE) && memcmp(payload, TEST_MESSAGE, len) == 0) {
            printf("[Client] <<< Test PASSED: Echo message matches original. >>>\n");
            g_test_passed = 1;
        } else {
            printf("[Client] <<< Test FAILED: Echo message does NOT match. >>>\n");
        }
    }
}

void client_on_error(void* user_data, int err_code, const char* message) {
    fprintf(stderr, "[Client] ERROR: %s\n", message);
}

int main() {
    printf("--- Starting Server ---\n");
    server_context_t* server_ctx = server_create(8080, 10);
    assert(server_ctx != NULL);
    server_register_connect_callback(server_ctx, server_on_connect, NULL);
    server_register_complete_message_callback(server_ctx, server_on_complete_message, NULL);
    server_register_disconnect_callback(server_ctx, server_on_disconnect, NULL);

    int ret = server_start(server_ctx);
    assert(ret == 0);
    sleep(1);

    printf("\n--- Starting Client ---\n");
    client_context_t* client_ctx = client_connect("127.0.0.1", 8080);
    assert(client_ctx != NULL);
    client_register_complete_message_callback(client_ctx, client_on_complete_message, NULL);
    client_register_error_callback(client_ctx, client_on_error, NULL);
    client_start_chat(client_ctx);
    sleep(1);

    printf("\n--- Running Test ---\n");
    printf("[Client] Sending test message: %s\n", TEST_MESSAGE);
    // 반드시 payload 길이와 함께 전송
    client_send_payload(client_ctx, MSG_TYPE_CHAT_TEXT, (const uint8_t*)TEST_MESSAGE, strlen(TEST_MESSAGE));

    printf("\n--- Verifying and Tearing Down ---\n");
    sleep(2);

    client_disconnect(client_ctx);
    server_shutdown(server_ctx);

    if (g_test_passed) {
        printf("\n✅ All tests passed successfully!\n");
        return 0;
    } else {
        printf("\n❌ Test failed.\n");
        return 1;
    }
}
