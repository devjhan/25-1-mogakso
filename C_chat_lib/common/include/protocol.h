//
// Created by jhan_macbook on 25. 6. 30.
//

#ifndef PROTOCOL_H
#define PROTOCOL_H

#ifdef __cplusplus
extern "C"
{
    #endif
    #include <stdint.h>
    #include <stddef.h>
    #define HEADER_SIZE 5

    /**
    * @brief 메시지 타입 열거형
    * @warning enum 값이 uint8_t 범위(0-255)를 초과하는 경우,
    *          frame_message()에서 프레임에 저장될 때는 하위 바이트만 저장됩니다.
    *          예: MSG_TYPE_PONG (901) → 프레임에서는 133 (0x85)로 저장됨
    *          parse_stream()에서 파싱할 때도 동일한 값이 반환됩니다.
    *          따라서 900 이상의 enum 값은 실제로는 132-133으로 처리됩니다.
    * @note 향후 버전에서는 enum 값 범위를 uint8_t 범위로 제한하는 것을 고려해야 합니다.
    */
    typedef enum
    {
        MSG_TYPE_CHAT_TEXT = 1,

        MSG_TYPE_FILE_INFO = 10,
        MSG_TYPE_FILE_CHUNK = 11,
        MSG_TYPE_FILE_END = 12,
        MSG_TYPE_FILE_REQUEST = 13,

        MSG_TYPE_USER_LOGIN_REQUEST = 100,
        MSG_TYPE_USER_LOGIN_RESPONSE = 101,

        MSG_TYPE_USER_JOIN_NOTICE = 200,
        MSG_TYPE_USER_LEAVE_NOTICE = 201,
        MSG_TYPE_SERVER_NOTICE = 202,

        MSG_TYPE_ERROR_RESPONSE = 500,

        MSG_TYPE_PING = 900,    /**< 실제 프레임에서는 132 (0x84)로 저장됨 */
        MSG_TYPE_PONG = 901,    /**< 실제 프레임에서는 133 (0x85)로 저장됨 */
    } message_type_t;

    typedef enum
    {
        PARSER_STATE_WANT_HEADER,
        PARSER_STATE_WANT_PAYLOAD,
    } parser_state_t;

    typedef void (*on_complete_callback)(void* user_data, message_type_t msg_type, const uint8_t* data, const size_t len);

    /**
    * @brief 프로토콜 프레임을 생성합니다.
    * @param type 프레임의 타입을 정의
    * @param payload 실제 데이터가 담긴 bytestream
    * @param payload_len payload의 길이
    * @param out_buffer 생성될 프레임이 저장될 출력 버퍼
    * @param buffer_len out_buffer의 길이
    * @return 성공할 경우 전체 프레임의 길이, 이외의 경우 -1을 반환합니다.
    */
    int frame_message(const message_type_t type, const uint8_t* payload, const size_t payload_len, uint8_t* out_buffer, const size_t buffer_len);

    typedef struct
    {
        parser_state_t parser_state;
        uint8_t header_buffer[HEADER_SIZE];
        size_t header_bytes_received;
        message_type_t pending_msg_type;
        uint32_t pending_msg_len;
        uint8_t* payload_buffer;
        size_t payload_bytes_received;
        void* user_data;
    } stream_parser_t;

    /**
    * @brief stream parser 객체를 생성합니다.
    * @param parser 초기화할 stream parser의 주소
    */
    void init_parser(stream_parser_t* parser);

    /**
    * @brief stream parser 객체를 파괴합니다.
    * @param parser 파괴할 stream parser의 주소
    */
    void destroy_parser(stream_parser_t* parser);

    /**
    * @brief stream을 파싱합니다.
    * @param parser 사용할 stream parser의 주소
    * @param data 파싱할 bytestream
    * @param len data의 길이
    * @param on_complete_cb 파싱이 완료되었을 경우 호출할 함수 포인터
    * @param user_data 콜백 함수 호출 시 첫번째 인자로 전달될 사용자 정의 데이터
    */
    int parse_stream(stream_parser_t* parser, const uint8_t* data, const size_t len, on_complete_callback on_complete_cb, void* user_data);

    #ifdef __cplusplus
}
#endif
#endif //PROTOCOL_H

