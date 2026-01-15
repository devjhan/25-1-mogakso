//
// 테스트 프레임워크 구현
//

#include "test_framework.h"
#include <stdlib.h>
#include <string.h>

// 전역 테스트 통계
test_stats_t g_test_stats = {0};

void test_init(const char* suite_name) {
    memset(&g_test_stats, 0, sizeof(test_stats_t));
    g_test_stats.suite_name = suite_name;
    g_test_stats.test_failed = 0;
    printf("\n=== Running Test Suite: %s ===\n\n", suite_name);
}

void test_finish(void) {
    printf("\n=== Test Summary: %s ===\n", g_test_stats.suite_name);
    printf("Total:  %d\n", g_test_stats.total_tests);
    printf("Passed: %d\n", g_test_stats.passed_tests);
    printf("Failed: %d\n", g_test_stats.failed_tests);
    
    if (g_test_stats.failed_tests > 0) {
        printf("\nFailed tests:\n");
        if (g_test_stats.failed_test_name) {
            printf("  - %s: %s (%s:%d)\n", 
                   g_test_stats.failed_test_name, 
                   g_test_stats.failure_message ? g_test_stats.failure_message : "Unknown error",
                   g_test_stats.failure_file ? g_test_stats.failure_file : "unknown",
                   g_test_stats.failure_line);
        }
        printf("\n❌ %d test(s) failed.\n", g_test_stats.failed_tests);
    } else {
        printf("\n✅ All tests passed!\n");
    }
    printf("\n");
}

int test_get_exit_code(void) {
    return (g_test_stats.failed_tests == 0) ? 0 : 1;
}
