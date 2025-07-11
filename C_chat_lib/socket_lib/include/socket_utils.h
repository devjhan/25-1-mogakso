//
// Created by jhan_macbook on 25. 6. 26.
//

#ifndef SOCKET_UTILS_H
#define SOCKET_UTILS_H

#ifdef __cplusplus
extern "C" {
 #endif

 /**
 * @brief TCP/IP 통신을 위한 소켓을 생성합니다.
 * @return 성공 시 소켓 파일 디스크립터, 실패 시 -1. errno가 설정됩니다.
 */
 int create_tcp_socket(void);

 /**
  * @brief 소켓의 SO_REUSEADDR 옵션을 활성화합니다.
  * @param sockfd 옵션을 설정할 소켓의 파일 디스크립터
  * @return 성공 시 0, 실패 시 -1. errno가 설정됩니다.
  */
 int set_socket_reusable(const int sockfd);

 /**
 * @brief 사용이 끝난 소켓을 안전하게 닫습니다.
 * @param sockfd 닫을 소켓의 파일 디스크립터
 * @return 성공 시 0, 실패 시 -1. errno가 설정됩니다.
 */
 int close_socket(const int sockfd);
 #ifdef __cplusplus
 }
#endif

#endif //SOCKET_UTILS_H

