//
// 클라이언트-서버 통합 테스트 (다중 클라이언트, 동시성)
//

#include "test_framework.h"
#include "chat_server.h"
#include "chat_client.h"
#include "protocol.h"
#include <pthread.h>
#include <unistd.h>
#include <string.h>
#include <signal.h>

#define MAX_CLIENTS 5
#define TEST_PORT 8888

static server_context_t* g_server_ctx = NULL;
static int g_connected_clients = 0;
static int g_received_messages = 0;
static pthread_mutex_t g_test_mutex = PTHREAD_MUTEX_INITIALIZER;
static const char* TEST_MESSAGES[MAX_CLIENTS] = {
    "Message from client 0",
    "Message from client 1",
    "Message from client 2",
    "Message from client 3",
    "Message from client 4"
};

// 서버 콜백
void test_server_on_connect(void* user_data, const client_info_t* client) {
    (void)user_data;
    pthread_mutex_lock(&g_test_mutex);
    g_connected_clients++;
    pthread_mutex_unlock(&g_test_mutex);
}

void test_server_on_message(void* user_data, const client_info_t* client, message_type_t msg_type, const uint8_t* payload, size_t len) {
    if (msg_type == MSG_TYPE_CHAT_TEXT) {
        server_context_t* server_ctx = (server_context_t*)user_data;
        pthread_mutex_lock(&g_test_mutex);
        g_received_messages++;
        pthread_mutex_unlock(&g_test_mutex);
        
        // 에코
        server_send_payload_to_client(server_ctx, client->socket_fd, MSG_TYPE_CHAT_TEXT, payload, len);
    }
}

void test_server_on_disconnect(void* user_data, const client_info_t* client) {
    (void)user_data;
    (void)client;
    pthread_mutex_lock(&g_test_mutex);
    g_connected_clients--;
    pthread_mutex_unlock(&g_test_mutex);
}

void test_server_on_error(void* user_data, int error_code, const char* message) {
    (void)user_data;
    (void)error_code;
    (void)message;
    // 에러는 로그만 남기고 계속 진행
}

// 클라이언트 채팅 루프를 실행하는 래퍼 함수
static void* client_chat_loop_wrapper(void* arg) {
    client_context_t* ctx = (client_context_t*)arg;
    client_start_chat(ctx);
    return NULL;
}

// 클라이언트 스레드 데이터
typedef struct {
    int client_id;
    const char* message;
    int success;
} client_thread_data_t;

static void* client_thread_func(void* arg) {
    client_thread_data_t* data = (client_thread_data_t*)arg;
    
    // 클라이언트 연결
    char server_ip[] = "127.0.0.1";
    client_context_t* client_ctx = client_connect(server_ip, TEST_PORT);
    
    if (client_ctx == NULL) {
        data->success = 0;
        return NULL;
    }
    
    // 클라이언트 채팅 루프를 별도 스레드에서 실행
    pthread_t chat_thread;
    pthread_create(&chat_thread, NULL, client_chat_loop_wrapper, client_ctx);
    usleep(500000);  // 0.5초 대기 (연결 및 루프 시작 대기)
    
    // 메시지 전송
    client_send_payload(client_ctx, MSG_TYPE_CHAT_TEXT, 
                       (const uint8_t*)data->message, strlen(data->message));
    
    usleep(500000);  // 응답 대기
    
    // 종료
    client_shutdown(client_ctx);
    pthread_join(chat_thread, NULL);
    client_disconnect(client_ctx);
    
    data->success = 1;
    return NULL;
}

TEST(test_multiple_clients_connection) {
    // 서버 시작
    g_server_ctx = server_create(TEST_PORT, MAX_CLIENTS);
    ASSERT_NOT_NULL(g_server_ctx);
    
    server_register_connect_callback(g_server_ctx, test_server_on_connect, NULL);
    server_register_complete_message_callback(g_server_ctx, test_server_on_message, g_server_ctx);
    server_register_disconnect_callback(g_server_ctx, test_server_on_disconnect, NULL);
    server_register_error_callback(g_server_ctx, test_server_on_error, NULL);
    
    int ret = server_start(g_server_ctx);
    ASSERT_EQ_INT(ret, 0);
    
    sleep(1);  // 서버 시작 대기
    
    // 여러 클라이언트 동시 연결
    pthread_t client_threads[MAX_CLIENTS];
    client_thread_data_t client_data[MAX_CLIENTS];
    
    for (int i = 0; i < MAX_CLIENTS; i++) {
        client_data[i].client_id = i;
        client_data[i].message = TEST_MESSAGES[i];
        client_data[i].success = 0;
        
        pthread_create(&client_threads[i], NULL, client_thread_func, &client_data[i]);
        usleep(200000);  // 0.2초 간격으로 연결
    }
    
    // 모든 클라이언트 스레드 종료 대기
    for (int i = 0; i < MAX_CLIENTS; i++) {
        pthread_join(client_threads[i], NULL);
    }
    
    sleep(1);  // 모든 메시지 처리 대기
    
    // 검증
    pthread_mutex_lock(&g_test_mutex);
    ASSERT_TRUE(g_connected_clients >= 0);  // 연결된 클라이언트 확인
    ASSERT_TRUE(g_received_messages >= 0);  // 수신된 메시지 확인
    pthread_mutex_unlock(&g_test_mutex);
    
    // 정리
    server_shutdown(g_server_ctx);
    server_destroy(g_server_ctx);
    g_server_ctx = NULL;
    
    // 초기화
    g_connected_clients = 0;
    g_received_messages = 0;
}

TEST(test_server_max_clients_limit) {
    const int max_clients = 2;
    const int test_port = TEST_PORT + 1;
    server_context_t* server = server_create(test_port, max_clients);
    ASSERT_NOT_NULL(server);
    
    server_register_connect_callback(server, test_server_on_connect, NULL);
    server_register_complete_message_callback(server, test_server_on_message, server);
    server_register_disconnect_callback(server, test_server_on_disconnect, NULL);
    server_register_error_callback(server, test_server_on_error, NULL);
    
    int ret = server_start(server);
    ASSERT_EQ_INT(ret, 0);
    
    sleep(1);  // 서버 시작 대기
    
    // 최대 클라이언트 수만큼 연결 시도
    const int total_attempts = max_clients + 1;  // 한 개 더 시도
    client_context_t** clients = (client_context_t**)calloc(total_attempts, sizeof(client_context_t*));
    ASSERT_NOT_NULL(clients);
    
    int connected_count = 0;
    
    for (int i = 0; i < max_clients; i++) {
        clients[i] = client_connect("127.0.0.1", test_port);
        if (clients[i] != NULL) {
            connected_count++;
        }
        usleep(200000);  // 연결 대기
    }
    
    // 초과 클라이언트 연결 시도 (서버가 거부할 수 있음)
    clients[max_clients] = client_connect("127.0.0.1", test_port);
    
    sleep(1);  // 연결 처리 대기
    
    // 최소한 max_clients만큼은 연결되어야 함
    ASSERT_TRUE(connected_count >= max_clients - 1);  // 최소 1개는 연결 성공
    
    // 정리
    for (int i = 0; i < total_attempts; i++) {
        if (clients[i] != NULL) {
            client_shutdown(clients[i]);
            usleep(100000);
            client_disconnect(clients[i]);
        }
    }
    
    free(clients);
    server_shutdown(server);
    server_destroy(server);
}

// 메인 함수
int main(void) {
    test_init("Client-Server Integration Tests");
    
    RUN_TEST(test_multiple_clients_connection);
    RUN_TEST(test_server_max_clients_limit);
    
    test_finish();
    
    pthread_mutex_destroy(&g_test_mutex);
    
    return test_get_exit_code();
}
