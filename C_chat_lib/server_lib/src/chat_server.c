//
// Created by jhan_macbook on 25. 6. 26.
//

#include "chat_server.h"
#include "socket_utils.h"
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <arpa/inet.h>
#define BUFFER_SIZE 4096

static void _def_on_client_connect_cb(void* user_data, const client_info_t* client);
static void _def_on_complete_message_cb(void* user_data, const client_info_t* client, const message_type_t msg_type, const uint8_t* payload, const size_t len);
static void _def_on_client_disconnect_cb(void* user_data, const client_info_t* client);
static void _def_on_error_cb(void* user_data, const int error_code, const char* message);
static void _on_internal_parse_complete_cb(void* user_data, const message_type_t msg_type, const uint8_t* payload, const size_t len);

void server_register_connect_callback(server_context_t* stx, const server_on_client_connected_callback callback, void* user_data)
{
    if (stx)
    {
        stx->on_connect_cb = callback ? callback : _def_on_client_connect_cb;
        stx->connect_user_data = user_data;
    }
}

void server_register_complete_message_callback(server_context_t* stx, const server_on_complete_message_received_callback callback, void* user_data)
{
    if (stx)
    {
        stx->on_complete_message_cb = callback ? callback : _def_on_complete_message_cb;
        stx->completed_message_user_data = user_data;
    }
}

void server_register_disconnect_callback(server_context_t* stx, const server_on_client_disconnected_callback callback, void* user_data)
{
    if (stx)
    {
        stx->on_disconnect_cb = callback ? callback : _def_on_client_disconnect_cb;
        stx->disconnect_user_data = user_data;
    }
}

void server_register_error_callback(server_context_t* stx, const server_on_error_callback callback, void* user_data)
{
    if (stx)
    {
        stx->on_error_cb = callback ? callback : _def_on_error_cb;
        stx->error_user_data = user_data;
    }
}

/**
 * @brief 내부 에러 처리 헬퍼 함수
 * @param stx 서버 컨텍스트 (NULL일 수 있음)
 * @param client 문제가 발생한 클라이언트(NULL일 수 있음)
 * @param user_msg 사용자 정의 오류 메시지
 * @param err_code errno 값 또는 사용자 정의 오류 코드
 */
static void _handle_error(const server_context_t* stx, const client_info_t* client, const char* user_msg, const int err_code)
{
    const char* sys_msg = strerror(err_code);

    char base_msg[BUFFER_SIZE];
    snprintf(base_msg, sizeof(base_msg), "%s: %s", user_msg, sys_msg);

    char final_msg[BUFFER_SIZE * 2];

    if (client == NULL)
    {
        snprintf(final_msg, sizeof(final_msg), "Fatal Server Error (code %d): %s", err_code, base_msg);
    } else
    {
        snprintf(final_msg, sizeof(final_msg), "Error with client %s (fd: %d) (code %d) - %s", client->ip_addr, client->socket_fd, err_code, base_msg);
    }

    if (stx && stx->on_error_cb)
    {
        stx->on_error_cb(stx->error_user_data, err_code, final_msg);
    } else
    {
        fprintf(stderr, "%s\n", final_msg);
    }
}

static void _cleanup_server_context(server_context_t* stx);

server_context_t* server_create(const int port, const int max_clients)
{
    server_context_t* stx = (server_context_t*)calloc(1, sizeof(server_context_t));

    if (max_clients <= 0)
    {
        _handle_error(NULL, NULL, "server_create: max_clients must be a positive integer", EINVAL);
        goto FAIL;
    }

    if (stx == NULL)
    {
        _handle_error(NULL, NULL, "server_create : calloc() for context failed.", errno);
        goto FAIL;
    }

    stx->listening_socket_fd = -1;
    stx->shutdown_pipe[0] = -1;
    stx->shutdown_pipe[1] = -1;

    stx->listening_socket_fd = create_tcp_socket();

    if (stx->listening_socket_fd < 0)
    {
        _handle_error(stx, NULL, "server_create: create_tcp_socket() failed", errno);
        goto FAIL;
    }

    if (set_socket_reusable(stx->listening_socket_fd) < 0)
    {
        _handle_error(stx, NULL, "server_create: set_socket_reusable() failed", errno);
        goto FAIL;
    }

    struct sockaddr_in server_addr =
    {
        .sin_family = AF_INET,
        .sin_addr.s_addr = htonl(INADDR_ANY),
        .sin_port = htons(port)
    };
    if (bind(stx->listening_socket_fd, (struct sockaddr*)&server_addr, sizeof(server_addr)) < 0)
    {
        _handle_error(stx, NULL,  "server_create: bind() failed", errno);
        goto FAIL;
    }

    if (listen(stx->listening_socket_fd, max_clients) < 0)
    {
        _handle_error(stx, NULL, "server_create: listen() failed", errno);
        goto FAIL;
    }

    stx->port = port;
    stx->max_clients = max_clients;
    stx->client_count = 0;

    stx->clients = (client_info_t*)calloc(max_clients, sizeof(client_info_t));
    stx->pollers = (struct pollfd*)calloc(max_clients + 2, sizeof(struct pollfd));

    if (stx->clients == NULL || stx->pollers == NULL)
    {
        _handle_error(stx, NULL, "server_create: calloc() for clients/pollers failed.", ENOMEM);
        goto FAIL;
    }

    stx->pollers[0].fd = stx->listening_socket_fd;
    stx->pollers[0].events = POLLIN;

    if (pipe(stx->shutdown_pipe) == -1)
    {
        _handle_error(stx, NULL, "server_create: pipe() for shutdown pipe failed.", errno);
        goto FAIL;
    }

    stx->pollers[1].fd = stx->shutdown_pipe[0];
    stx->pollers[1].events = POLLIN;

    for (int i = 2; i < max_clients + 2; ++i)
    {
        stx->pollers[i].fd = -1;
    }

    stx->server_thread = 0;
    stx->server_state = SERVER_STATE_STOPPED;
    stx->mutex_inited = 0;

    if (pthread_mutex_init(&stx->state_mutex, NULL) != 0)
    {
        _handle_error(stx, NULL, "server_create: pthread_mutex_init() failed", errno);
        goto FAIL;
    }

    stx->mutex_inited = 1;

    server_register_connect_callback(stx, NULL, NULL);
    server_register_complete_message_callback(stx, NULL, NULL);
    server_register_disconnect_callback(stx, NULL, NULL);
    server_register_error_callback(stx, NULL, NULL);
    return stx;

    FAIL:
    _cleanup_server_context(stx);
    return NULL;
}

void server_shutdown(server_context_t* stx)
{
    if (stx == NULL)
    {
        return;
    }
    pthread_mutex_lock(&stx->state_mutex);

    if (stx->server_state != SERVER_STATE_RUNNING)
    {
        pthread_mutex_unlock(&stx->state_mutex);
        return;
    }

    stx->server_state = SERVER_STATE_SHUTTING_DOWN;
    pthread_mutex_unlock(&stx->state_mutex);

    const char shutdown_signal = 'x';
    ssize_t bytes_written;

    do
    {
        bytes_written = write(stx->shutdown_pipe[1], &shutdown_signal, 1);
    } while (bytes_written == -1 && errno == EINTR);

    if (bytes_written == -1)
    {
        _handle_error(stx, NULL, "server_shutdown: write() to shutdown pipe failed.", errno);
    }

    if (stx->server_thread != 0)
    {
        pthread_join(stx->server_thread, NULL);
    }
}

void server_destroy(server_context_t* stx)
{
    _cleanup_server_context(stx);
}

/**
 * @brief 서버 컨텍스트에 할당된 모든 자원을 해제하고 정리합니다. (내부용)
 * @param stx 서버 컨텍스트
 */
static void _cleanup_server_context(server_context_t* stx)
{
    if (stx == NULL)
    {
        return;
    }
    if (stx->pollers != NULL)
    {
        free(stx->pollers);
    }
    if (stx->clients != NULL)
    {
        for (int i = 0; i < stx->max_clients; ++i)
        {
            if (stx->clients[i].client_parser != NULL)
            {
                destroy_parser(stx->clients[i].client_parser);
                free(stx->clients[i].client_parser);
            }
        }
        free(stx->clients);
    }
    if (stx->shutdown_pipe[0] >= 0)
    {
        close(stx->shutdown_pipe[0]);
    }
    if (stx->shutdown_pipe[1] >= 0)
    {
        close(stx->shutdown_pipe[1]);
    }
    if (stx->listening_socket_fd >= 0)
    {
        close_socket(stx->listening_socket_fd);
    }
    if (stx->mutex_inited)
    {
        pthread_mutex_destroy(&stx->state_mutex);
    }
    free(stx);
}

static void* _server_run(void* arg);
int server_start(server_context_t* stx)
{
    if (stx == NULL)
    {
        _handle_error(NULL, NULL, "server_start : context is NULL.", EINVAL);
        return -1;
    }

    pthread_mutex_lock(&stx->state_mutex);

    if (stx->server_state != SERVER_STATE_STOPPED)
    {
        _handle_error(stx, NULL, "server_start: server is already running or not in a stoppable state.", EALREADY);
        pthread_mutex_unlock(&stx->state_mutex);
        return -1;
    }

    stx->server_state = SERVER_STATE_RUNNING;

    const int thread_err = pthread_create(&stx->server_thread, NULL, _server_run, stx);

    if (thread_err != 0)
    {
        char err_msg[BUFFER_SIZE];
        snprintf(err_msg, sizeof(err_msg), "server_start: pthread_create() failed: %s", strerror(thread_err));

        _handle_error(stx, NULL, err_msg, thread_err);

        stx->server_state = SERVER_STATE_STOPPED;
        pthread_mutex_unlock(&stx->state_mutex);
        return -1;
    }
    pthread_mutex_unlock(&stx->state_mutex);
    return 0;
}

static void _add_client(server_context_t* stx);
static void _handle_client_data(server_context_t* stx, const int poller_index);
static void _remove_client(server_context_t* stx, const int poller_index);

/**
* @brief 서버의 메인 이벤트 루프. 별도의 쓰레드에서 호출됩니다.
* @details server_start 함수에 의해 내부적으로 호출되는 쓰레드 함수입니다.
*          server_start 함수 이외의 방법으로 호출하지 마십시오.
* @param arg 서버 컨텍스트
*/
static void* _server_run(void* arg)
{
    server_context_t* stx = (server_context_t*)arg;

    if (stx == NULL)
    {
        _handle_error(NULL, NULL, "_server_run : stx is NULL.", EINVAL);
        return NULL;
    }

    char is_running = 1;
    while (is_running)
    {
        const int poll_count = poll(stx->pollers, stx->max_clients + 2, -1);

        if (poll_count < 0)
        {
            if (errno == EINTR)
            {
                continue;
            }
            _handle_error(stx, NULL, "_server_run : poll() failed.", errno);
            is_running = 0;
            break;
        }

        if (stx->pollers[0].revents & POLLIN)
        {
            _add_client(stx);
        }

        if (stx->pollers[1].revents & POLLIN)
        {
            is_running = 0;
            break;
        }

        for (int i = 2; i < stx->max_clients + 2; ++i)
        {
            if (stx->pollers[i].revents)
            {
                _handle_client_data(stx, i);
            }
        }
    }
    pthread_mutex_lock(&stx->state_mutex);
    stx->server_state = SERVER_STATE_STOPPED;
    pthread_mutex_unlock(&stx->state_mutex);
    return NULL;
}

/**
* @brief 새로운 클라이언트 연결을 처리하는 헬퍼 함수 (내부용)
* @param stx 서버 컨텍스트
*/
static void _add_client(server_context_t* stx)
{
    struct sockaddr_in client_addr;
    socklen_t client_len = sizeof(client_addr);

    const int client_fd = accept(stx->listening_socket_fd, (struct sockaddr*)&client_addr, &client_len);
    if (client_fd < 0)
    {
        _handle_error(stx, NULL, "_handle_new_connection: accept() failed", errno);
        return;
    }

    if (stx->client_count >= stx->max_clients)
    {
        const char* msg = "server is already filled up. please try again later.";
        send(client_fd, msg, strlen(msg), 0);
        close_socket(client_fd);
        return;
    }

    int poller_index = -1;

    for (int i = 2; i < stx->max_clients + 2; ++i)
    {
        if (stx->pollers[i].fd == -1)
        {
            poller_index = i;
            break;
        }
    }

    if (poller_index == -1)
    {
        close_socket(client_fd);
        return;
    }

    stx->pollers[poller_index].fd = client_fd;
    stx->pollers[poller_index].events = POLLIN;

    const int client_index = poller_index - 2;
    stx->clients[client_index].socket_fd = client_fd;
    inet_ntop(AF_INET, &client_addr.sin_addr, stx->clients[client_index].ip_addr, sizeof(stx->clients[client_index].ip_addr));
    stx->clients[client_index].client_parser = (stream_parser_t*)calloc(1, sizeof(stream_parser_t));

    if (stx->clients[client_index].client_parser == NULL)
    {
        _handle_error(stx, NULL, "_add_client: Failed to allocate memory for parser", ENOMEM);
        close_socket(client_fd);
        stx->pollers[poller_index].fd = -1;
        return;
    }
    init_parser(stx->clients[client_index].client_parser);
    ++stx->client_count;

    stx->on_connect_cb(stx->connect_user_data, &stx->clients[client_index]);
}

/**
 * @brief 기존 클라이언트로부터 온 데이터를 처리하는 헬퍼 함수 (내부용)
 * @param stx 서버 컨텍스트
 * @param poller_index 클라이언트 인덱스
 */
static void _handle_client_data(server_context_t* stx, const int poller_index)
{
    client_info_t* client = &stx->clients[poller_index - 2];
    stream_parser_t* parser = client->client_parser;

    if (stx->pollers[poller_index].revents & POLLIN)
    {
        uint8_t buffer[BUFFER_SIZE];
        const ssize_t bytes_received = recv(client->socket_fd, buffer, BUFFER_SIZE - 1, 0);
        message_context_t mtx =
        {
            .server_context = stx,
            .client_info = client,
        };
        if (bytes_received > 0)
        {
            if (parse_stream(parser, buffer, bytes_received, _on_internal_parse_complete_cb, &mtx) < 0)
            {
                _handle_error(stx, client, "_handle_client_data : parse_stream() failed.", 0);
                _remove_client(stx, poller_index);
            }
        } else if (bytes_received == 0)
        {
            stx->on_disconnect_cb(stx->disconnect_user_data, client);
            _remove_client(stx, poller_index);
        } else
        {
            _handle_error(stx, client, "_handle_client_data: recv() failed", errno);
            stx->on_disconnect_cb(stx->disconnect_user_data, client);
            _remove_client(stx, poller_index);
        }
    } else if (stx->pollers[poller_index].revents & (POLLERR | POLLHUP | POLLNVAL))
    {
        _handle_error(stx, client, "_handle_client_data: socket error detected by poll()", 0);
        stx->on_disconnect_cb(stx->disconnect_user_data, client);
        _remove_client(stx, poller_index);
    }
}

/**
 * @brief 기존 클라이언트와의 연결을 끊는 헬퍼 함수 (내부용)
 * @param stx 서버 컨텍스트
 * @param poller_index 클라이언트 인덱스
 */
static void _remove_client(server_context_t* stx, const int poller_index)
{
    close_socket(stx->pollers[poller_index].fd);

    stx->pollers[poller_index].fd = -1;
    stx->pollers[poller_index].revents = 0;

    client_info_t* client = &stx->clients[poller_index - 2];
    if (client->client_parser != NULL)
    {
        destroy_parser(client->client_parser);
        free(client->client_parser);
        client->client_parser = NULL;
    }

    memset(client, 0, sizeof(client_info_t));
    stx->client_count--;
}

void server_send_payload_to_client(const int client_fd, const message_type_t msg_type, const uint8_t* payload, const size_t payload_len)
{
    if (client_fd < 0 || payload == NULL)
    {
        _handle_error(NULL, NULL, "server_send_payload_to_client: invalid arguments provided.", EINVAL);
        return;
    }

    uint8_t frame_buffer[BUFFER_SIZE];
    const int frame_len = frame_message(msg_type, payload, payload_len, frame_buffer, sizeof(frame_buffer));

    if (frame_len < 0)
    {
        _handle_error(NULL, NULL, "server_send_payload_to_client: frame_message() failed.", 0);
        return;
    }

    size_t sent_len = 0;

    while (sent_len < frame_len)
    {
        const ssize_t bytes_sent = send(client_fd, frame_buffer + sent_len, frame_len - sent_len, MSG_NOSIGNAL);

        if (bytes_sent < 0)
        {
            _handle_error(NULL, NULL, "server_send_payload_to_client: send() failed", errno);
            return;
        }

        if (bytes_sent == 0)
        {
            _handle_error(NULL, NULL, "server_send_payload_to_client: send() returned 0, connection may be closed.", 0);
            return;
        }

        sent_len += bytes_sent;
    }

    if (sent_len < frame_len)
    {
        _handle_error(NULL, NULL, "client_send_chat_message: failed to send the entire payload.", 0);
    }
}

void server_broadcast_message(const server_context_t* stx, const message_type_t msg_type, const uint8_t* payload, const size_t payload_len, const int exclude_fd)
{
    if (stx == NULL || payload == NULL)
    {
        _handle_error(NULL, NULL, "server_broadcast_message: invalid arguments provided.", EINVAL);
        return;
    }

    for (int i = 1; i < stx->max_clients + 1; ++i)
    {
        const int client_fd = stx->pollers[i].fd;

        if (client_fd >= 0)
        {
            if (client_fd != exclude_fd)
            {
                server_send_payload_to_client(client_fd, msg_type, payload, payload_len);
            }
        }
    }
}

/**
 * @brief on_connect_cb의 기본값 (내부용)
 * @param user_data 식별자
 * @param client 클라이언트
 */
static void _def_on_client_connect_cb(void* user_data, const client_info_t* client)
{
    const char* who = (const char*)user_data;

    if (who && *who)
    {
        fprintf(stderr, "[%s] ", who);
    }

    if (client != NULL)
    {
        fprintf(stderr, "Client connected: fd=%d, ip=%s\n", client->socket_fd, client->ip_addr);
    }
    else
    {
        fprintf(stderr, "Client connected: (client_info_t is NULL)\n");
    }
}

/**
 * @brief on_complete_message_cb의 기본값 (내부용)
 * @param user_data 식별자
 * @param client 클라이언트
 * @param msg_type 메시지의 타입
 * @param payload 전송받은 bytestream
 * @param len payload의 길이
 */
static void _def_on_complete_message_cb(void* user_data, const client_info_t* client, const message_type_t msg_type, const uint8_t* payload, const size_t len)
{
    const char* who = (const char*)user_data;
    if (who && *who)
    {
        fprintf(stderr, "[%s] ", who);
    }

    if (client != NULL)
    {
        fprintf(stderr, "Message received: fd=%d, ip=%s, type=%d, len=%zu\n", client->socket_fd, client->ip_addr, (int)msg_type, len);
    }
    else
    {
        fprintf(stderr, "Message received: (client_info_t is NULL), type=%d, len=%zu\n", (int)msg_type, len);
    }

    if (payload && len > 0)
    {
        if (msg_type == MSG_TYPE_CHAT_TEXT)
        {
            const size_t print_len = len < 256 ? len : 255;
            char tmp[256];
            memcpy(tmp, payload, print_len);
            tmp[print_len] = '\0';
            fprintf(stderr, "Payload (as text, first %zu bytes): \"%s\"\n", print_len, tmp);
        } else {
            const size_t print_len = len < 32 ? len : 32;
            fprintf(stderr, "Payload (hex, first %zu bytes):", print_len);

            for (size_t i = 0; i < print_len; ++i)
            {
                fprintf(stderr, " %02X", payload[i]);
            }
            fprintf(stderr, "\n");
        }
    }
}

/**
 * @brief on_disconnect_cb의 기본값 (내부용)
 * @param user_data 식별자
 * @param client 클라이언트
 */
static void _def_on_client_disconnect_cb(void* user_data, const client_info_t* client)
{
    const char* who = (const char*)user_data;
    if (who && *who)
    {
        fprintf(stderr, "[%s] ", who);
    }

    if (client != NULL)
    {
        fprintf(stderr, "client disconnected: fd=%d, ip=%s\n", client->socket_fd, client->ip_addr);
    } else
    {
        fprintf(stderr, "client disconnected: (client_info_t is NULL)\n");
    }
}

/**
 * @brief on_error_cb의 기본값 (내부용)
 * @param user_data 식별자
 * @param error_code 에러 코드
 * @param message 출력할 내용
 */
static void _def_on_error_cb(void* user_data, const int error_code, const char* message)
{
    const char* who = (const char*)user_data;

    if (who && *who)
    {
        fprintf(stderr, "[%s] ", who);
    }
    fprintf(stderr, "ERROR (code=%d): %s\n", error_code, message ? message : "(no message)");
}

/**
 * @brief on_parse_complete_cb의 기본값 (내부용)
 * @param user_data message_context_t 타입의 구조체. server_context_t, client_info_t를 멤버로 가짐
 * @param msg_type 메시지의 종류
 * @param payload bytestream
 * @param len payload의 길이
 */
static void _on_internal_parse_complete_cb(void* user_data, const message_type_t msg_type, const uint8_t* payload, const size_t len)
{
    const message_context_t* mtx = (message_context_t*)user_data;
    const server_context_t* stx = mtx->server_context;
    const client_info_t* client = mtx->client_info;

    if (msg_type == MSG_TYPE_PING)
    {
        server_send_payload_to_client(client->socket_fd, MSG_TYPE_PONG, NULL, 0);
        return;
    }

    stx->on_complete_message_cb(stx->completed_message_user_data, client, msg_type, payload, len);
}