//
// Created by jhan_macbook on 25. 6. 30.
//
#include "protocol.h"
#include <stdlib.h>
#include <arpa/inet.h>
#include <string.h>

/**
 * @brief stream parser의 상태를 초기화하는 헬퍼 함수(내부용)
 * @param parser 초기화할 stream parser
 */
static void _reset_parser(stream_parser_t* parser)
{
    parser->parser_state = PARSER_STATE_WANT_HEADER;
    parser->header_bytes_received = 0;
    parser->pending_msg_len = 0;

    if (parser->payload_buffer != NULL)
    {
        free(parser->payload_buffer);
        parser->payload_buffer = NULL;
    }
    parser->payload_bytes_received = 0;
}

int frame_message(const message_type_t type, const uint8_t* payload, const size_t payload_len, uint8_t* out_buffer, const size_t buffer_len)
{
    if (out_buffer == NULL)
    {
        return -1;
    }

    const size_t frame_size = HEADER_SIZE + payload_len;

    if (buffer_len < frame_size)
    {
        return -1;
    }

    const uint32_t net_payload_len = htonl((uint32_t)payload_len);

    out_buffer[0] = (uint8_t)type;
    memcpy(out_buffer + 1, &net_payload_len, sizeof(net_payload_len));

    if (payload != NULL && payload_len > 0)
    {
        memcpy(out_buffer + HEADER_SIZE, payload, payload_len);
    }
    return (int)frame_size;
}

void init_parser(stream_parser_t* parser)
{
    if (parser == NULL)
    {
        return;
    }
    memset(parser, 0, sizeof(stream_parser_t));
    parser->parser_state = PARSER_STATE_WANT_HEADER;
}

void destroy_parser(stream_parser_t* parser)
{
    if (parser == NULL)
    {
        return;
    }
    if (parser->payload_buffer != NULL)
    {
        free(parser->payload_buffer);
        parser->payload_buffer = NULL;
    }
}

int parse_stream(stream_parser_t* parser, const uint8_t* data, const size_t len, on_complete_callback on_complete_cb, void* user_data)
{
    if (parser == NULL || data == NULL || on_complete_cb == NULL)
    {
        return -1;
    }

    size_t bytes_processed = 0;

    while (bytes_processed < len)
    {
        if (parser->parser_state == PARSER_STATE_WANT_HEADER)
        {
            const size_t bytes_needed = HEADER_SIZE - parser->header_bytes_received;
            const size_t bytes_to_copy = (len - bytes_processed < bytes_needed) ? (len - bytes_processed) : bytes_needed;

            memcpy(parser->header_buffer + parser->header_bytes_received, data + bytes_processed, bytes_to_copy);
            parser->header_bytes_received += bytes_to_copy;
            bytes_processed += bytes_to_copy;

            if (parser->header_bytes_received == HEADER_SIZE)
            {
                parser->pending_msg_type = (message_type_t)parser->header_buffer[0];

                uint32_t net_len;
                memcpy(&net_len, parser->header_buffer + 1, sizeof(uint32_t));
                parser->pending_msg_len = ntohl(net_len);

                if (parser->pending_msg_len == 0)
                {
                    on_complete_cb(user_data, parser->pending_msg_type, NULL, 0);
                    _reset_parser(parser);
                } else
                {
                    parser->payload_buffer = (uint8_t*)calloc(1, parser->pending_msg_len);

                    if (parser->payload_buffer == NULL)
                    {
                        return -1;
                    }
                    parser->payload_bytes_received = 0;
                    parser->parser_state = PARSER_STATE_WANT_PAYLOAD;
                }
            }
        } else if (parser->parser_state == PARSER_STATE_WANT_PAYLOAD)
        {
            const size_t bytes_needed = parser->pending_msg_len - parser->payload_bytes_received;
            const size_t bytes_to_copy = (len - bytes_processed < bytes_needed) ? (len - bytes_processed) : bytes_needed;

            memcpy(parser->payload_buffer + parser->payload_bytes_received, data + bytes_processed, bytes_to_copy);
            parser->payload_bytes_received += bytes_to_copy;
            bytes_processed += bytes_to_copy;

            if (parser->payload_bytes_received == parser->pending_msg_len)
            {
                on_complete_cb(user_data, parser->pending_msg_type, parser->payload_buffer, parser->pending_msg_len);
                _reset_parser(parser);
            }
        }
    }
    return 0;
}
