//
// Created by jhan_macbook on 25. 7. 11.
//

#ifndef COMMAND_QUEUE_H
#define COMMAND_QUEUE_H

#ifdef __cplusplus
extern "C"
{
    #endif

    typedef struct command_queue_t command_queue_t;

    /**
    * @brief 새로운 커맨드 큐를 생성하고 초기화합니다.
    * @return 성공 시 생성된 큐의 포인터, 실패 시 NULL을 반환합니다.
    */
    command_queue_t* queue_create(void);

    /**
    * @brief 큐를 파괴하고 모든 관련 리소스를 해제합니다.
    * @param q 파괴할 큐
    * @param data_destroy_func 큐에 남아있는 각 데이터 요소를 해제하기 위한 함수 포인터.
    *                          NULL일 경우 데이터 자체는 해제하지 않습니다.
    */
    void queue_destroy(command_queue_t* q, void (*data_destroy_func)(void* data));

    /**
     * @brief 큐의 끝에 새로운 데이터를 추가합니다. (Thread-Safe)
    * @param q 데이터를 추가할 큐
    * @param data 추가할 데이터 포인터
    */
    void queue_push(command_queue_t* q, void* data);

    /**
    * @brief 큐의 앞에서 데이터를 꺼냅니다. (Thread-Safe)
    * @param q 데이터를 꺼낼 큐
    * @return 큐에서 꺼낸 데이터 포인터. 큐가 비어있으면 NULL을 반환합니다.
    */
    void* queue_pop(command_queue_t* q);

    /**
    * @brief 큐가 비어있는지 확인합니다. (Thread-Safe)
    * @param q 확인할 큐
    * @return 큐가 비어있으면 1, 그렇지 않으면 0을 반환합니다.
    */
    int queue_is_empty(command_queue_t* q);

    #ifdef __cplusplus
}
#endif
#endif //COMMAND_QUEUE_H
