//
// 간단한 테스트 프레임워크 헤더 파일
// 각 테스트 케이스는 TEST() 매크로로 정의되고, ASSERT() 매크로로 검증합니다.
//

#ifndef TEST_FRAMEWORK_H
#define TEST_FRAMEWORK_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stddef.h>

// 테스트 통계
typedef struct {
    const char* suite_name;
    int total_tests;
    int passed_tests;
    int failed_tests;
    int test_failed;  // bool 대신 int 사용 (C99 호환성)
    const char* failed_test_name;
    const char* failure_message;
    int failure_line;
    const char* failure_file;
} test_stats_t;

// 전역 테스트 통계
extern test_stats_t g_test_stats;

// 테스트 프레임워크 초기화/정리
void test_init(const char* suite_name);
void test_finish(void);
int test_get_exit_code(void);

// 테스트 실행 매크로
#define TEST(name) \
    static void test_##name(void); \
    void test_##name(void)

// 테스트 실행 헬퍼
#define RUN_TEST(name) \
    do { \
        printf("Running test: %s\n", #name); \
        g_test_stats.total_tests++; \
        g_test_stats.test_failed = 0; \
        test_##name(); \
        if (g_test_stats.test_failed) { \
            g_test_stats.failed_tests++; \
            printf("  [FAIL] %s\n", #name); \
        } else { \
            g_test_stats.passed_tests++; \
            printf("  [PASS] %s\n", #name); \
        } \
    } while(0)

// Assert 매크로
#define ASSERT(condition, message) \
    do { \
        if (!(condition)) { \
            g_test_stats.test_failed = 1; \
            g_test_stats.failed_test_name = __func__; \
            g_test_stats.failure_message = message ? message : "Assertion failed: " #condition; \
            g_test_stats.failure_line = __LINE__; \
            g_test_stats.failure_file = __FILE__; \
            fprintf(stderr, "  [FAIL] %s:%d: %s\n", __FILE__, __LINE__, g_test_stats.failure_message); \
            return; \
        } \
    } while(0)

#define ASSERT_TRUE(condition) \
    ASSERT((condition), "Expected true, got false: " #condition)

#define ASSERT_FALSE(condition) \
    ASSERT(!(condition), "Expected false, got true: " #condition)

#define ASSERT_EQ(a, b) \
    ASSERT((a) == (b), "Expected " #a " == " #b " but got different values")

#define ASSERT_NE(a, b) \
    ASSERT((a) != (b), "Expected " #a " != " #b " but got same values")

#define ASSERT_NULL(ptr) \
    ASSERT((ptr) == NULL, "Expected NULL pointer")

#define ASSERT_NOT_NULL(ptr) \
    ASSERT((ptr) != NULL, "Expected non-NULL pointer")

#define ASSERT_STREQ(a, b) \
    ASSERT(strcmp((a), (b)) == 0, "Expected strings to be equal: " #a " and " #b)

#define ASSERT_MEMEQ(a, b, len) \
    ASSERT(memcmp((a), (b), (len)) == 0, "Expected memory regions to be equal")

#define ASSERT_EQ_INT(a, b) \
    do { \
        int _a = (a); \
        int _b = (b); \
        if (_a != _b) { \
            fprintf(stderr, "  [FAIL] %s:%d: Expected %d == %d\n", __FILE__, __LINE__, _a, _b); \
            g_test_stats.test_failed = 1; \
            g_test_stats.failed_test_name = __func__; \
            g_test_stats.failure_line = __LINE__; \
            g_test_stats.failure_file = __FILE__; \
            return; \
        } \
    } while(0)

#define ASSERT_EQ_SIZE(a, b) \
    do { \
        size_t _a = (a); \
        size_t _b = (b); \
        if (_a != _b) { \
            fprintf(stderr, "  [FAIL] %s:%d: Expected %zu == %zu\n", __FILE__, __LINE__, _a, _b); \
            g_test_stats.test_failed = 1; \
            g_test_stats.failed_test_name = __func__; \
            g_test_stats.failure_line = __LINE__; \
            g_test_stats.failure_file = __FILE__; \
            return; \
        } \
    } while(0)

#endif // TEST_FRAMEWORK_H
