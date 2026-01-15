//
// Created by jhan_macbook on 25. 6. 26.
//

#ifndef CHAT_CLIENT_H
#define CHAT_CLIENT_H

#ifdef __cplusplus
extern "C"
{
    #endif

    #include "protocol.h"
    #include <poll.h>

    typedef void (*client_on_complete_message_received_callback)(void* user_data, const message_type_t msg_type, const uint8_t* payload, const size_t len);
    typedef void (*client_on_error_callback)(void* user_data, int error_code, const char* message);

    typedef struct
    {
        int socket_fd;
        const char* server_ip;
        int server_port;
        struct pollfd pollers[2];
        int shutdown_pipe[2];
        client_on_complete_message_received_callback on_complete_message_cb;
        void* complete_message_user_data;
        client_on_error_callback on_error_cb;
        void* error_user_data;
        stream_parser_t* client_parser;
    } client_context_t;

    /**
    * @brief 메시지 수신 시 호출될 콜백 함수를 등록합니다.
    * @param ctx 클라이언트 컨텍스트
    * @param callback 호출될 함수 포인터
    * @param user_data 콜백 함수 호출 시 첫 번째 인자로 전달될 사용자 정의 데이터
    */
    void client_register_complete_message_callback(client_context_t* ctx, const client_on_complete_message_received_callback callback, void* user_data);

    /**
    * @brief 에러 발생 시 호출될 콜백 함수를 등록합니다.
    * @param ctx 클라이언트 컨텍스트
    * @param callback 호출될 함수 포인터
    * @param user_data 콜백 함수 호출 시 첫 번째 인자로 전달될 사용자 정의 데이터
    */
    void client_register_error_callback(client_context_t* ctx, const client_on_error_callback callback, void* user_data);

    /**
    * @brief 클라이언트 컨텍스트를 초기화하고 서버에 연결합니다.
    * @param ip 서버 IP 주소 문자열
    * @param port 서버 포트 번호
    * @return 성공 시 초기화된 client_context_t 포인터, 실패 시 NULL
    */
    client_context_t* client_connect(const char* ip, int port);

    /**
    * @brief 지정된 파일을 서버로 전송합니다.
    * @param ctx 클라이언트 컨텍스트
    * @param filepath 전송할 파일의 경로
    * @return 성공 시 0, 실패 시 -1
    */
    int client_send_file(const client_context_t* ctx, const char* filepath);

    /**
    * @brief 실시간 채팅 루프를 시작합니다.
    * @details 이 함수는 블로킹(blocking) 함수입니다. 내부적으로 poll()을 사용하여
    *          무한 루프로 메시지를 수신 대기합니다. 
    *          함수는 client_shutdown()이 호출되거나 연결이 종료될 때까지 반환하지 않습니다.
    * 
    * @warning 이 함수를 메인 스레드에서 호출하면 이후 코드가 실행되지 않습니다.
    *          별도의 스레드에서 실행하거나, 비동기 처리가 필요한 경우 적절한 스레드 관리를 수행해야 합니다.
    * 
    * @param ctx 클라이언트 컨텍스트
    * 
    * @example
    * // 올바른 사용법 (별도 스레드에서 실행)
    * void* chat_thread(void* arg) {
    *     client_context_t* ctx = (client_context_t*)arg;
    *     client_start_chat(ctx);
    *     return NULL;
    * }
    * pthread_t thread;
    * pthread_create(&thread, NULL, chat_thread, client_ctx);
    */
    void client_start_chat(client_context_t* ctx);

    /**
    * @brief ctx에 shutdown flag를 설정합니다.
    * @param ctx 클라이언트 컨텍스트
    */
    void client_shutdown(client_context_t* ctx);

    /**
    * @brief 클라이언트의 모든 자원을 해제하고 연결을 종료합니다.
    * @param ctx 클라이언트 컨텍스트
    */
    void client_disconnect(client_context_t* ctx);

    /**
    * @brief payload를 서버로 전송합니다.
    * @details 네트워크 상황에 따라 데이터가 분할 전송될 수 있는 경우를 처리합니다.
    * @param ctx 클라이언트 컨텍스트
    * @param msg_type 전송할 payload의 타입
    * @param payload 전송할 bytestream
    * @param payload_len payload의 길이
    */
    void client_send_payload(const client_context_t* ctx, const message_type_t msg_type, const uint8_t* payload, const size_t payload_len);
    #ifdef __cplusplus
}
#endif
#endif //CHAT_CLIENT_H
