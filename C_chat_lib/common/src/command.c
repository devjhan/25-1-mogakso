//
// Created by jhan_macbook on 25. 7. 11.
//

#include "command.h"

#include <stdlib.h>
#include <string.h>

command_t* create_send_command(const int client_fd, const message_type_t msg_type, const uint8_t* payload, const size_t len)
{
    command_t* cmd = (command_t*)calloc(1, sizeof(command_t));

    if (cmd == NULL)
    {
        return NULL;
    }

    cmd->type = CMD_SEND_MESSAGE;
    send_command_t* send_cmd = &cmd->data.send_cmd;
    send_cmd->target_client_fd = client_fd;
    send_cmd->msg_type = msg_type;
    send_cmd->payload_len = len;

    if (len > 0 && payload != NULL)
    {
        send_cmd->payload = (uint8_t*)malloc(len);
        if (send_cmd->payload == NULL)
        {
            free(cmd);
            return NULL;
        }
        memcpy(send_cmd->payload, payload, len);
    } else
    {
        send_cmd->payload = NULL;
    }
    return cmd;
}

command_t* create_broadcast_command(message_type_t msg_type, const uint8_t* payload, size_t len, int exclude_fd)
{
    command_t* cmd = (command_t*)calloc(1, sizeof(command_t));

    if (cmd == NULL)
    {
        return NULL;
    }

    cmd->type = CMD_BROADCAST_MESSAGE;
    broadcast_command_t* broadcast_cmd = &cmd->data.broadcast_cmd;
    broadcast_cmd->msg_type = msg_type;
    broadcast_cmd->payload_len = len;
    broadcast_cmd->exclude_client_fd = exclude_fd;

    if (len > 0 && payload != NULL)
    {
        broadcast_cmd->payload = (uint8_t*)malloc(len);
        if (broadcast_cmd->payload == NULL)
        {
            free(cmd);
            return NULL;
        }
        memcpy(broadcast_cmd->payload, payload, len);
    } else
    {
        broadcast_cmd->payload = NULL;
    }

    return cmd;
}


void destroy_command(void* cmd_ptr)
{
    if (cmd_ptr == NULL)
    {
        return;
    }

    command_t* cmd = (command_t*)cmd_ptr;

    switch (cmd->type)
    {
        case CMD_SEND_MESSAGE:
        {
            const send_command_t* send_cmd = &cmd->data.send_cmd;

            if (send_cmd->payload != NULL)
            {
                free(send_cmd->payload);
            }
            break;
        }
        case CMD_BROADCAST_MESSAGE:
        {
            const broadcast_command_t* broadcast_cmd = &cmd->data.broadcast_cmd;

            if (broadcast_cmd->payload != NULL)
            {
                free(broadcast_cmd->payload);
            }
            break;
        }
    }
    free(cmd);
}
