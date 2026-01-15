//
// Socket Utils 모듈 단위 테스트
//

#include "test_framework.h"
#include "socket_utils.h"
#include <unistd.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <netinet/in.h>
#include <errno.h>

// 소켓 생성 테스트
TEST(test_create_tcp_socket_success) {
    int sockfd = create_tcp_socket();
    
    ASSERT_TRUE(sockfd >= 0);  // 유효한 파일 디스크립터
    ASSERT_TRUE(sockfd < 1024 || sockfd >= 0);  // 일반적으로 작은 값
    
    // 소켓 타입 확인 (getsockopt로 확인 가능)
    int sock_type;
    socklen_t len = sizeof(sock_type);
    int result = getsockopt(sockfd, SOL_SOCKET, SO_TYPE, &sock_type, &len);
    ASSERT_EQ_INT(result, 0);
    ASSERT_EQ_INT(sock_type, SOCK_STREAM);  // TCP 소켓
    
    close_socket(sockfd);
}

TEST(test_create_tcp_socket_multiple) {
    // 여러 소켓을 생성할 수 있는지 테스트
    int sockfds[10];
    int success_count = 0;
    
    for (int i = 0; i < 10; i++) {
        sockfds[i] = create_tcp_socket();
        if (sockfds[i] >= 0) {
            success_count++;
        }
    }
    
    ASSERT_EQ_INT(success_count, 10);  // 모두 성공해야 함
    
    // 모든 소켓 닫기
    for (int i = 0; i < 10; i++) {
        if (sockfds[i] >= 0) {
            close_socket(sockfds[i]);
        }
    }
}

// SO_REUSEADDR 설정 테스트
TEST(test_set_socket_reusable_success) {
    int sockfd = create_tcp_socket();
    ASSERT_TRUE(sockfd >= 0);
    
    int result = set_socket_reusable(sockfd);
    ASSERT_EQ_INT(result, 0);  // 성공해야 함
    
    // SO_REUSEADDR 옵션이 실제로 설정되었는지 확인
    int reuse;
    socklen_t len = sizeof(reuse);
    int get_result = getsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &reuse, &len);
    ASSERT_EQ_INT(get_result, 0);
    ASSERT_TRUE(reuse != 0);  // 0이 아니어야 함 (설정됨)
    
    close_socket(sockfd);
}

TEST(test_set_socket_reusable_invalid_fd) {
    int invalid_fd = -1;
    int result = set_socket_reusable(invalid_fd);
    ASSERT_TRUE(result < 0);  // 실패해야 함
    ASSERT_EQ_INT(errno, EBADF);  // 잘못된 파일 디스크립터
}

TEST(test_set_socket_reusable_closed_fd) {
    int sockfd = create_tcp_socket();
    ASSERT_TRUE(sockfd >= 0);
    
    close_socket(sockfd);
    
    // 이미 닫힌 소켓에 대해 옵션 설정 시도
    int result = set_socket_reusable(sockfd);
    ASSERT_TRUE(result < 0);  // 실패해야 함
}

// 소켓 닫기 테스트
TEST(test_close_socket_success) {
    int sockfd = create_tcp_socket();
    ASSERT_TRUE(sockfd >= 0);
    
    int result = close_socket(sockfd);
    ASSERT_EQ_INT(result, 0);  // 성공해야 함
    
    // 이미 닫힌 소켓에 다시 닫기 시도
    result = close_socket(sockfd);
    ASSERT_TRUE(result < 0);  // 실패해야 함
}

TEST(test_close_socket_invalid_fd) {
    int invalid_fd = -1;
    int result = close_socket(invalid_fd);
    ASSERT_TRUE(result < 0);  // 실패해야 함
}

TEST(test_close_socket_already_closed) {
    int sockfd = create_tcp_socket();
    ASSERT_TRUE(sockfd >= 0);
    
    close_socket(sockfd);
    
    // 이미 닫힌 소켓 다시 닫기
    int result = close_socket(sockfd);
    ASSERT_TRUE(result < 0);  // 실패해야 함
}

// 실제 사용 시나리오 테스트
TEST(test_socket_lifecycle) {
    int sockfd = create_tcp_socket();
    ASSERT_TRUE(sockfd >= 0);
    
    // SO_REUSEADDR 설정
    int result = set_socket_reusable(sockfd);
    ASSERT_EQ_INT(result, 0);
    
    // 바인드 테스트를 위한 주소 설정
    struct sockaddr_in addr;
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons(0);  // 시스템이 포트 할당
    
    result = bind(sockfd, (struct sockaddr*)&addr, sizeof(addr));
    ASSERT_EQ_INT(result, 0);  // 바인드 성공
    
    // 소켓 닫기
    result = close_socket(sockfd);
    ASSERT_EQ_INT(result, 0);
}

TEST(test_socket_reuse_after_close) {
    struct sockaddr_in addr;
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons(9999);  // 고정 포트
    
    // 첫 번째 소켓 생성 및 바인드
    int sockfd1 = create_tcp_socket();
    ASSERT_TRUE(sockfd1 >= 0);
    
    int result = set_socket_reusable(sockfd1);
    ASSERT_EQ_INT(result, 0);
    
    result = bind(sockfd1, (struct sockaddr*)&addr, sizeof(addr));
    ASSERT_EQ_INT(result, 0);
    
    close_socket(sockfd1);
    
    // SO_REUSEADDR이 설정되어 있으면 같은 포트를 즉시 재사용 가능
    usleep(100000);  // 0.1초 대기 (TIME_WAIT 상태 고려)
    
    int sockfd2 = create_tcp_socket();
    ASSERT_TRUE(sockfd2 >= 0);
    
    result = set_socket_reusable(sockfd2);
    ASSERT_EQ_INT(result, 0);
    
    // SO_REUSEADDR이 설정되어 있으면 즉시 재바인드 가능
    result = bind(sockfd2, (struct sockaddr*)&addr, sizeof(addr));
    ASSERT_EQ_INT(result, 0);  // 성공해야 함
    
    close_socket(sockfd2);
}

// 에러 처리 테스트
TEST(test_socket_operations_error_handling) {
    // 잘못된 파일 디스크립터로 모든 작업 시도
    int invalid_fd = 99999;
    
    int result = set_socket_reusable(invalid_fd);
    ASSERT_TRUE(result < 0);
    
    result = close_socket(invalid_fd);
    ASSERT_TRUE(result < 0);
}

// 메인 함수
int main(void) {
    test_init("Socket Utils Tests");
    
    RUN_TEST(test_create_tcp_socket_success);
    RUN_TEST(test_create_tcp_socket_multiple);
    RUN_TEST(test_set_socket_reusable_success);
    RUN_TEST(test_set_socket_reusable_invalid_fd);
    RUN_TEST(test_set_socket_reusable_closed_fd);
    RUN_TEST(test_close_socket_success);
    RUN_TEST(test_close_socket_invalid_fd);
    RUN_TEST(test_close_socket_already_closed);
    RUN_TEST(test_socket_lifecycle);
    RUN_TEST(test_socket_reuse_after_close);
    RUN_TEST(test_socket_operations_error_handling);
    
    test_finish();
    
    return test_get_exit_code();
}
