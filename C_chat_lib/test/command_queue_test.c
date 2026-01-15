//
// Command Queue 모듈 단위 테스트
//

#include "test_framework.h"
#include "command_queue.h"
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>

// 기본 큐 동작 테스트
TEST(test_queue_create_destroy) {
    command_queue_t* queue = queue_create();
    
    ASSERT_NOT_NULL(queue);
    ASSERT_TRUE(queue_is_empty(queue));
    
    queue_destroy(queue, NULL);
}

TEST(test_queue_push_pop) {
    command_queue_t* queue = queue_create();
    ASSERT_NOT_NULL(queue);
    
    int* data1 = (int*)malloc(sizeof(int));
    int* data2 = (int*)malloc(sizeof(int));
    *data1 = 42;
    *data2 = 100;
    
    ASSERT_TRUE(queue_is_empty(queue));
    
    queue_push(queue, data1);
    ASSERT_FALSE(queue_is_empty(queue));
    
    queue_push(queue, data2);
    ASSERT_FALSE(queue_is_empty(queue));
    
    int* popped1 = (int*)queue_pop(queue);
    ASSERT_NOT_NULL(popped1);
    ASSERT_EQ_INT(*popped1, 42);
    free(popped1);
    
    ASSERT_FALSE(queue_is_empty(queue));
    
    int* popped2 = (int*)queue_pop(queue);
    ASSERT_NOT_NULL(popped2);
    ASSERT_EQ_INT(*popped2, 100);
    free(popped2);
    
    ASSERT_TRUE(queue_is_empty(queue));
    
    void* empty_pop = queue_pop(queue);
    ASSERT_NULL(empty_pop);
    
    queue_destroy(queue, NULL);
}

TEST(test_queue_multiple_elements) {
    command_queue_t* queue = queue_create();
    ASSERT_NOT_NULL(queue);
    
    const int num_elements = 100;
    // VLA 경고 방지를 위해 동적 할당 사용
    int** values = (int**)malloc(num_elements * sizeof(int*));
    ASSERT_NOT_NULL(values);
    
    // 100개 요소 추가
    for (int i = 0; i < num_elements; i++) {
        values[i] = (int*)malloc(sizeof(int));
        *values[i] = i;
        queue_push(queue, values[i]);
    }
    
    ASSERT_FALSE(queue_is_empty(queue));
    
    // 순서대로 제거
    for (int i = 0; i < num_elements; i++) {
        int* popped = (int*)queue_pop(queue);
        ASSERT_NOT_NULL(popped);
        ASSERT_EQ_INT(*popped, i);
        free(popped);
    }
    
    ASSERT_TRUE(queue_is_empty(queue));
    
    queue_destroy(queue, NULL);
}

TEST(test_queue_destroy_with_callback) {
    command_queue_t* queue = queue_create();
    ASSERT_NOT_NULL(queue);
    
    int* data1 = (int*)malloc(sizeof(int));
    int* data2 = (int*)malloc(sizeof(int));
    *data1 = 1;
    *data2 = 2;
    
    queue_push(queue, data1);
    queue_push(queue, data2);
    
    // destroy 시 각 요소에 대해 콜백 호출
    queue_destroy(queue, free);
    // data1, data2는 free()로 해제됨 (메모리 릭 검사는 valgrind로 확인)
}

TEST(test_queue_null_safety) {
    queue_destroy(NULL, NULL);
    
    command_queue_t* queue = queue_create();
    queue_push(NULL, NULL);
    queue_push(queue, NULL); // NULL 데이터도 허용
    
    void* result = queue_pop(NULL);
    ASSERT_NULL(result);
    
    int empty = queue_is_empty(NULL);
    ASSERT_TRUE(empty);
    
    queue_destroy(queue, NULL);
}

TEST(test_queue_fifo_order) {
    command_queue_t* queue = queue_create();
    ASSERT_NOT_NULL(queue);
    
    const char* messages[] = {"first", "second", "third", "fourth"};
    const int count = 4;
    
    for (int i = 0; i < count; i++) {
        char* msg = strdup(messages[i]);
        queue_push(queue, msg);
    }
    
    for (int i = 0; i < count; i++) {
        char* popped = (char*)queue_pop(queue);
        ASSERT_NOT_NULL(popped);
        ASSERT_STREQ(popped, messages[i]);
        free(popped);
    }
    
    queue_destroy(queue, NULL);
}

// 메인 함수
int main(void) {
    test_init("Command Queue Tests");
    
    RUN_TEST(test_queue_create_destroy);
    RUN_TEST(test_queue_push_pop);
    RUN_TEST(test_queue_multiple_elements);
    RUN_TEST(test_queue_destroy_with_callback);
    RUN_TEST(test_queue_null_safety);
    RUN_TEST(test_queue_fifo_order);
    
    test_finish();
    
    return test_get_exit_code();
}
