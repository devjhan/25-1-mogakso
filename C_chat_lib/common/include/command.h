//
// Created by jhan_macbook on 25. 7. 11.
//

#ifndef COMMAND_H
#define COMMAND_H

#ifdef __cplusplus
extern "C"
{
    #endif
    #include <protocol.h>

    typedef enum
    {
        CMD_SEND_MESSAGE,
        CMD_BROADCAST_MESSAGE,
    } command_type_t;

    typedef struct
    {
        int target_client_fd;
        message_type_t msg_type;
        uint8_t* payload;
        size_t payload_len;
    } send_command_t;

    typedef struct {
        message_type_t msg_type;
        uint8_t* payload;
        size_t payload_len;
        int exclude_client_fd;
    } broadcast_command_t;

    typedef struct
    {
        command_type_t type;
        union
        {
            send_command_t send_cmd;
            broadcast_command_t broadcast_cmd;
        } data;
    } command_t;

    /**
    * @brief 커맨드 객체를 생성하고 초기화.
    * @param client_fd 메시지를 보낼 대상 클라이언트의 소켓 fd
    * @param msg_type 프로토콜에 정의된 메시지 타입
    * @param payload 메시지 페이로드 데이터
    * @param len 페이로드의 길이
    * @return 성공 시 생성된 커맨드 포인터, 실패 시 NULL
    */
    command_t* create_send_command(const int client_fd, message_type_t msg_type, const uint8_t* payload, const size_t len);

    /**
    * @brief 브로드캐스트 커맨드 객체를 생성하고 초기화
    * @param msg_type 프로토콜에 정의된 메시지 타입
    * @param payload 메시지 페이로드 데이터
    * @param len 페이로드의 길이
    * @param exclude_fd 브로드캐스트에서 제외할 클라이언트 fd
    * @return 성공 시 생성된 커맨드 포인터, 실패 시 NULL
    */
    command_t* create_broadcast_command(message_type_t msg_type, const uint8_t* payload, size_t len, int exclude_fd);

    /**
    * @brief 커맨드 객체와 그 내부의 동적 할당된 메모리를 안전하게 해제하는 함수
    * @param cmd_ptr 해제할 커맨드 포인터
    */
    void destroy_command(void* cmd_ptr);

    #ifdef __cplusplus
}
#endif
#endif //COMMAND_H
