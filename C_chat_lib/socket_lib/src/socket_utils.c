//
// Created by jhan_macbook on 25. 6. 26.
//
#include "socket_utils.h"
#include <sys/socket.h>
#include <unistd.h>

int create_tcp_socket(void)
{
    return socket(AF_INET, SOCK_STREAM, 0);
}

int set_socket_reusable(const int sockfd)
{
    const int optval = 1;
    return setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &optval, sizeof(optval));
}

int close_socket(const int sockfd)
{
    return close(sockfd);
}