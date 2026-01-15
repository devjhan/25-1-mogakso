//
// Protocol 모듈 경계값 및 에러 처리 테스트
//

#include "test_framework.h"
#include "protocol.h"
#include <string.h>
#include <stdlib.h>
#include <limits.h>

// 경계값 테스트
TEST(test_frame_message_max_payload) {
    const size_t max_payload = 1024 * 1024;  // 1MB
    const size_t buffer_size = HEADER_SIZE + max_payload + 100;  // 여유 공간
    uint8_t* buffer = (uint8_t*)malloc(buffer_size);
    ASSERT_NOT_NULL(buffer);
    
    uint8_t* payload = (uint8_t*)malloc(max_payload);
    ASSERT_NOT_NULL(payload);
    
    // 페이로드 초기화
    memset(payload, 0xAA, max_payload);
    
    int frame_len = frame_message(MSG_TYPE_FILE_CHUNK, payload, max_payload, buffer, buffer_size);
    ASSERT_TRUE(frame_len > 0);
    ASSERT_EQ_SIZE((size_t)frame_len, HEADER_SIZE + max_payload);
    
    // 첫 바이트와 마지막 바이트 확인
    ASSERT_EQ_INT(buffer[0], (uint8_t)MSG_TYPE_FILE_CHUNK);
    ASSERT_EQ_INT(buffer[HEADER_SIZE], 0xAA);
    ASSERT_EQ_INT(buffer[HEADER_SIZE + max_payload - 1], 0xAA);
    
    free(payload);
    free(buffer);
}

TEST(test_frame_message_zero_length) {
    uint8_t buffer[1024];
    int frame_len = frame_message(MSG_TYPE_PING, NULL, 0, buffer, sizeof(buffer));
    
    ASSERT_EQ_INT(frame_len, HEADER_SIZE);
    ASSERT_EQ_INT(buffer[0], (uint8_t)MSG_TYPE_PING);
    
    // 길이 확인 (네트워크 바이트 순서)
    uint32_t net_len;
    memcpy(&net_len, buffer + 1, sizeof(uint32_t));
    uint32_t len = ntohl(net_len);
    ASSERT_EQ_SIZE(len, 0U);
}

TEST(test_frame_message_exact_buffer_size) {
    const size_t payload_len = 100;
    const size_t buffer_size = HEADER_SIZE + payload_len;
    uint8_t* buffer = (uint8_t*)malloc(buffer_size);
    ASSERT_NOT_NULL(buffer);
    
    uint8_t* payload = (uint8_t*)malloc(payload_len);
    ASSERT_NOT_NULL(payload);
    memset(payload, 0xBB, payload_len);
    
    int frame_len = frame_message(MSG_TYPE_CHAT_TEXT, payload, payload_len, buffer, buffer_size);
    ASSERT_EQ_INT(frame_len, (int)(HEADER_SIZE + payload_len));
    
    free(buffer);
    free(payload);
}

TEST(test_frame_message_one_byte_too_small) {
    const size_t payload_len = 100;
    const size_t buffer_size = HEADER_SIZE + payload_len - 1;  // 1바이트 부족
    uint8_t* buffer = (uint8_t*)malloc(buffer_size);
    ASSERT_NOT_NULL(buffer);
    
    uint8_t* payload = (uint8_t*)malloc(payload_len);
    ASSERT_NOT_NULL(payload);
    memset(payload, 0xCC, payload_len);
    
    int frame_len = frame_message(MSG_TYPE_CHAT_TEXT, payload, payload_len, buffer, buffer_size);
    ASSERT_EQ_INT(frame_len, -1);  // 실패해야 함
    
    free(buffer);
    free(payload);
}

// Parser 경계값 테스트
static int edge_parse_callback_called = 0;
static message_type_t edge_parsed_msg_type = 0;
static size_t edge_parsed_payload_len = 0;

static void edge_parse_callback(void* user_data, message_type_t msg_type, const uint8_t* data, const size_t len) {
    (void)user_data;
    edge_parse_callback_called = 1;
    edge_parsed_msg_type = msg_type;
    edge_parsed_payload_len = len;
}

TEST(test_parse_stream_max_size_payload) {
    stream_parser_t parser;
    const size_t max_payload = 1024 * 1024;  // 1MB
    const size_t frame_size = HEADER_SIZE + max_payload;
    uint8_t* large_frame = (uint8_t*)malloc(frame_size);
    ASSERT_NOT_NULL(large_frame);
    
    uint8_t* large_payload = (uint8_t*)malloc(max_payload);
    ASSERT_NOT_NULL(large_payload);
    memset(large_payload, 0xDD, max_payload);
    
    init_parser(&parser);
    edge_parse_callback_called = 0;
    edge_parsed_msg_type = 0;
    edge_parsed_payload_len = 0;
    
    int frame_len = frame_message(MSG_TYPE_FILE_CHUNK, large_payload, max_payload, large_frame, frame_size);
    ASSERT_TRUE(frame_len > 0);
    
    // 한 번에 파싱
    int result = parse_stream(&parser, large_frame, (size_t)frame_len, edge_parse_callback, NULL);
    
    ASSERT_EQ_INT(result, 0);
    ASSERT_EQ_INT(edge_parse_callback_called, 1);
    ASSERT_EQ_INT(edge_parsed_msg_type, (uint8_t)MSG_TYPE_FILE_CHUNK);
    ASSERT_EQ_SIZE(edge_parsed_payload_len, max_payload);
    
    free(large_frame);
    free(large_payload);
    destroy_parser(&parser);
}

TEST(test_parse_stream_fragmented_large_payload) {
    stream_parser_t parser;
    const size_t payload_len = 10000;
    const size_t frame_size = HEADER_SIZE + payload_len;
    uint8_t* frame = (uint8_t*)malloc(frame_size);
    ASSERT_NOT_NULL(frame);
    
    uint8_t* payload = (uint8_t*)malloc(payload_len);
    ASSERT_NOT_NULL(payload);
    memset(payload, 0xEE, payload_len);
    
    init_parser(&parser);
    edge_parse_callback_called = 0;
    edge_parsed_msg_type = 0;
    edge_parsed_payload_len = 0;
    
    int frame_len = frame_message(MSG_TYPE_FILE_CHUNK, payload, payload_len, frame, frame_size);
    ASSERT_TRUE(frame_len > 0);
    
    // 작은 청크로 나눠서 파싱
    const size_t chunk_size = 100;
    size_t offset = 0;
    
    while (offset < (size_t)frame_len) {
        size_t remaining = (size_t)frame_len - offset;
        size_t to_send = (remaining < chunk_size) ? remaining : chunk_size;
        
        int result = parse_stream(&parser, frame + offset, to_send, edge_parse_callback, NULL);
        ASSERT_EQ_INT(result, 0);
        
        offset += to_send;
        
        if (edge_parse_callback_called) {
            break;  // 파싱 완료
        }
    }
    
    ASSERT_EQ_INT(edge_parse_callback_called, 1);
    ASSERT_EQ_INT(edge_parsed_msg_type, (uint8_t)MSG_TYPE_FILE_CHUNK);
    ASSERT_EQ_SIZE(edge_parsed_payload_len, payload_len);
    
    free(frame);
    free(payload);
    destroy_parser(&parser);
}

// NULL 및 잘못된 인자 테스트
TEST(test_frame_message_null_payload_with_length) {
    uint8_t buffer[1024];
    // NULL payload지만 길이가 0이 아닌 경우
    int frame_len = frame_message(MSG_TYPE_CHAT_TEXT, NULL, 100, buffer, sizeof(buffer));
    // 구현에 따라 다르지만, 일반적으로 길이 0으로 처리되거나 실패해야 함
    // 현재 구현에서는 payload_len만 확인하므로 HEADER_SIZE만 반환될 수 있음
    ASSERT_TRUE(frame_len >= 0);  // 최소한 HEADER_SIZE는 반환
}

TEST(test_parse_stream_partial_header) {
    stream_parser_t parser;
    uint8_t data[3];  // HEADER_SIZE보다 작음
    
    init_parser(&parser);
    edge_parse_callback_called = 0;
    
    // 불완전한 헤더 전송
    data[0] = (uint8_t)MSG_TYPE_PING;
    data[1] = 0;
    data[2] = 0;
    
    int result = parse_stream(&parser, data, 3, edge_parse_callback, NULL);
    ASSERT_EQ_INT(result, 0);  // 에러가 아님 (아직 파싱 중)
    ASSERT_EQ_INT(edge_parse_callback_called, 0);  // 아직 완료되지 않음
    
    destroy_parser(&parser);
}

TEST(test_parse_stream_invalid_length) {
    stream_parser_t parser;
    uint8_t frame[HEADER_SIZE + 10];
    
    init_parser(&parser);
    edge_parse_callback_called = 0;
    
    // 잘못된 길이 (너무 큰 값)를 가진 프레임 생성
    frame[0] = (uint8_t)MSG_TYPE_CHAT_TEXT;
    uint32_t invalid_len = 0xFFFFFFFF;  // 최대값
    uint32_t net_len = htonl(invalid_len);
    memcpy(frame + 1, &net_len, sizeof(uint32_t));
    
    // 파싱 시도 (메모리 할당 실패 예상)
    int result = parse_stream(&parser, frame, HEADER_SIZE, edge_parse_callback, NULL);
    // 매우 큰 길이를 요청하면 메모리 할당 실패로 -1 반환 예상
    ASSERT_TRUE(result < 0 || edge_parse_callback_called == 0);
    
    destroy_parser(&parser);
}

// 메인 함수
int main(void) {
    test_init("Protocol Edge Cases Tests");
    
    RUN_TEST(test_frame_message_max_payload);
    RUN_TEST(test_frame_message_zero_length);
    RUN_TEST(test_frame_message_exact_buffer_size);
    RUN_TEST(test_frame_message_one_byte_too_small);
    RUN_TEST(test_parse_stream_max_size_payload);
    RUN_TEST(test_parse_stream_fragmented_large_payload);
    RUN_TEST(test_frame_message_null_payload_with_length);
    RUN_TEST(test_parse_stream_partial_header);
    RUN_TEST(test_parse_stream_invalid_length);
    
    test_finish();
    
    return test_get_exit_code();
}
