//
// Created by jhan_macbook on 25. 6. 26.
//

#include "chat_client.h"
#include "socket_utils.h"
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#define BUFFER_SIZE 4096

static void _def_on_complete_message_cb(void* user_data, const message_type_t msg_type, const uint8_t* payload, const size_t len);
static void _def_on_error_cb(void* user_data, const int error_code, const char* message);

void client_register_complete_message_callback(client_context_t* ctx, const client_on_complete_message_received_callback callback, void* user_data)
{
    if (ctx)
    {
        ctx->on_complete_message_cb = callback ? callback : _def_on_complete_message_cb;
        ctx->complete_message_user_data = user_data;
    }
}

void client_register_error_callback(client_context_t* ctx, const client_on_error_callback callback, void* user_data) {
    if (ctx)
    {
        ctx->on_error_cb = callback ? callback : _def_on_error_cb;
        ctx->error_user_data = user_data;
    }
}

/**
 * @brief 내부 에러 처리 헬퍼 함수
 * @param ctx 클라이언트 컨텍스트 (NULL일 수 있음)
 * @param user_msg 사용자 정의 오류 메시지
 * @param err_code errno 값 또는 사용자 정의 오류 코드
 */
static void _handle_error(const client_context_t* ctx, const char* user_msg, const int err_code)
{
    const char* sys_msg = strerror(err_code);
    char full_msg[BUFFER_SIZE];

    snprintf(full_msg, sizeof(full_msg), "%s: %s", user_msg, sys_msg);

    if (ctx && ctx->on_error_cb)
    {
        ctx->on_error_cb(ctx->error_user_data, err_code, full_msg);
    } else
    {
        fprintf(stderr, "FATAL ERROR (no context or callback): %s\n", full_msg);
    }
}

static void _cleanup_client_context(client_context_t* ctx);
client_context_t* client_connect(const char* ip, int port)
{
    client_context_t* ctx = (client_context_t*)calloc(1, sizeof(client_context_t));

    if (ctx == NULL)
    {
        _handle_error(NULL, "client_connect: calloc() for context failed", errno);
        goto FAIL;
    }

    ctx->socket_fd = -1;
    ctx->shutdown_pipe[0] = -1;
    ctx->shutdown_pipe[1] = -1;

    ctx->socket_fd = create_tcp_socket();

    if (ctx->socket_fd < 0) {
        _handle_error(ctx, "client_connect: create_tcp_socket() failed", errno);
        goto FAIL;
    }

    struct sockaddr_in serv_addr =
    {
        .sin_family = AF_INET,
        .sin_port = htons(port),
    };

    if (inet_pton(AF_INET, ip, &serv_addr.sin_addr) <= 0)
    {
        _handle_error(ctx, "client_connect: inet_pton() failed", errno);
        goto FAIL;
    }

    if (connect(ctx->socket_fd, (struct sockaddr*)&serv_addr, sizeof(serv_addr)) < 0)
    {
        _handle_error(ctx, "client_connect: connect() failed", errno);
        goto FAIL;
    }

    ctx->server_ip = strdup(ip);

    if (ctx->server_ip == NULL) {
        _handle_error(ctx, "client_connect: strdup() for ip failed", errno);
        goto FAIL;
    }

    ctx->server_port = port;
    ctx->pollers[0].fd = ctx->socket_fd;
    ctx->pollers[0].events = POLLIN;

    if (pipe(ctx->shutdown_pipe) == -1)
    {
        _handle_error(ctx, "client_connect : pipe() for shutdown pipe failed", errno);
        goto FAIL;
    }
    ctx->pollers[1].fd = ctx->shutdown_pipe[0];
    ctx->pollers[1].events = POLLIN;

    ctx->client_parser = (stream_parser_t*)calloc(1, sizeof(stream_parser_t));

    if (ctx->client_parser == NULL)
    {
        _handle_error(ctx, "client connect : calloc() failed.", ENOMEM);
        goto FAIL;
    }
    init_parser(ctx->client_parser);

    client_register_complete_message_callback(ctx, NULL, NULL);
    client_register_error_callback(ctx, NULL, NULL);
    return ctx;

    FAIL:
    _cleanup_client_context(ctx);
    return NULL;
}

void client_shutdown(client_context_t* ctx)
{
    if (ctx == NULL)
    {
        return;
    }
    const char shutdown_signal = 'x';
    ssize_t bytes_written;

    do
    {
        bytes_written = write(ctx->shutdown_pipe[1], &shutdown_signal, 1);
    } while (bytes_written == -1 && errno == EINTR);

    if (bytes_written == -1)
    {
        _handle_error(ctx, "client_shutdown: write() for shutdown pipe failed.", errno);
    }
}

void client_disconnect(client_context_t* ctx)
{
    _cleanup_client_context(ctx);
}

/**
 * @brief 클라이언트 컨텍스트에 할당된 모든 자원을 해제하고 정리합니다. (내부용)
 * @param ctx 클라이언트 컨텍스트
 */
static void _cleanup_client_context(client_context_t* ctx)
{
    if (ctx == NULL)
    {
        return;
    }
    if (ctx->client_parser != NULL)
    {
        destroy_parser(ctx->client_parser);
        free(ctx->client_parser);
    }
    if (ctx->shutdown_pipe[0] >= 0)
    {
        close(ctx->shutdown_pipe[0]);
    }
    if (ctx->shutdown_pipe[1] >= 0)
    {
        close(ctx->shutdown_pipe[1]);
    }
    if (ctx->server_ip != NULL)
    {
        free((void*)ctx->server_ip);
    }
    if (ctx->socket_fd >= 0)
    {
        close_socket(ctx->socket_fd);
    }
    free(ctx);
}

void client_send_payload(const client_context_t* ctx, const message_type_t msg_type, const uint8_t* payload, const size_t payload_len)
{
    if (ctx == NULL || payload == NULL)
    {
        _handle_error(NULL, "client_send_payload: invalid argument(s) provided.", EINVAL);
        return;
    }
    uint8_t frame_buffer[BUFFER_SIZE];
    const int frame_len = frame_message(msg_type, payload, payload_len, frame_buffer, sizeof(frame_buffer));

    if (frame_len < 0)
    {
        _handle_error(ctx, "client_send_payload : frame_message() failed.", 0);
        return;
    }

    size_t sent_len = 0;

    while (sent_len < frame_len)
    {
        const ssize_t bytes_sent = send(ctx->socket_fd, frame_buffer + sent_len, frame_len - sent_len, 0);

        if (bytes_sent < 0)
        {
            _handle_error(ctx, "client_send_payload: send() failed.", errno);
            break;
        }

        if (bytes_sent == 0)
        {
            _handle_error(ctx, "client_send_payload: connection closed by peer.", 0);
            break;
        }
        sent_len += (size_t)bytes_sent;
    }

    if (sent_len < frame_len)
    {
        _handle_error(ctx, "client_send_payload: failed to send the entire payload.", 0);
    }
}

int client_send_file(const client_context_t* ctx, const char* filepath)
{
    FILE* fp = fopen(filepath, "rb");

    if (fp == NULL)
    {
        _handle_error(ctx, "client_send_file : fopen() failed.", errno);
        return -1;
    }

    fseek(fp, 0, SEEK_END);
    const size_t filesize = ftell(fp);
    fseek(fp, 0, SEEK_SET);

    char header[256];
    const size_t header_len = snprintf(header, sizeof(header), "{\"filename\":\"%s\",\"filesize\":%zu}", filepath, filesize);
    client_send_payload(ctx, MSG_TYPE_FILE_INFO, (const uint8_t*)header, header_len);

    uint8_t chunk[BUFFER_SIZE];
    size_t read_len;

    while ((read_len = fread(chunk, 1, sizeof(chunk), fp)) > 0)
    {
        client_send_payload(ctx, MSG_TYPE_FILE_CHUNK, chunk, read_len);
    }
    client_send_payload(ctx, MSG_TYPE_FILE_END, NULL, 0);
    fclose(fp);
    return 0;
}

static void _on_client_parse_complete_cb(void* user_data, const message_type_t msg_type, const uint8_t* payload, const size_t len);
void client_start_chat(client_context_t* ctx)
{
    if (ctx == NULL)
    {
        _handle_error(NULL, "client_start_chat: context is NULL.", EINVAL);
        return;
    }

    char is_running = 1;
    while(is_running)
    {
        const int poll_count = poll(ctx->pollers, 2, -1);

        if (poll_count < 0)
        {
            _handle_error(ctx, "client_start_chat: poll() failed", errno);
            return;
        }

        if (ctx->pollers[0].revents & (POLLIN | POLLHUP))
        {
            uint8_t buffer[BUFFER_SIZE];
            const ssize_t bytes_received = recv(ctx->socket_fd, buffer, BUFFER_SIZE - 1, 0);

            if (bytes_received > 0)
            {
                if (parse_stream(ctx->client_parser, buffer, bytes_received, _on_client_parse_complete_cb, ctx) < 0)
                {
                    _handle_error(ctx, "client_start_chat: parse_stream() failed", 0);
                    break;
                }
            } else
            {
                const char* msg = (bytes_received == 0) ? "client_start_chat: connection closed by server." : "client_start_chat: recv() failed.";
                ctx->on_complete_message_cb(ctx->complete_message_user_data, 0, (const uint8_t*)msg, strlen(msg));
                break;
            }
        }

        if (ctx->pollers[1].revents & POLLIN)
        {
            printf("client_start_chat : shutdown flag received via pipe.");
            is_running = 0;
        }
    }
    printf("client_start_chat : client chat loop has been terminated.");
}

/**
 * @brief  파싱이 완료되었을 때 호출되는 헬퍼 함수 (내부용)
 * @param user_data client_context_t 타입의 구조체
 * @param msg_type 메시지의 종류
 * @param payload bytestream
 * @param len payload의 길이
 */
static void _on_client_parse_complete_cb(void* user_data, const message_type_t msg_type, const uint8_t* payload, const size_t len)
{
    const client_context_t* ctx = (client_context_t*)user_data;
    ctx->on_complete_message_cb(ctx->complete_message_user_data, msg_type, payload, len);
}

/**
 * @brief on_complete_message_cb의 기본값 (내부용)
 * @details 아무런 동작을 하지 않습니다.
 */
static void _def_on_complete_message_cb(void* user_data, const message_type_t msg_type, const uint8_t* payload, const size_t len)
{
    (void)user_data;
    (void)msg_type;
    (void)payload;
    (void)len;
}

/**
 * @brief on_error_cb의 기본값 (내부용)
 * @details 아무런 동작을 하지 않습니다.
 */
static void _def_on_error_cb(void* user_data, const int error_code, const char* message)
{
    (void)user_data;
    (void)error_code;
    (void)message;
}