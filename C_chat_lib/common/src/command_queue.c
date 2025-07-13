//
// Created by jhan_macbook on 25. 7. 11.
//

#include "command_queue.h"
#include <pthread.h>
#include <stdlib.h>

typedef struct queue_node_t
{
    void* data;
    struct queue_node_t* next;
} queue_node_t;

struct command_queue_t
{
    queue_node_t* fake_head;
    queue_node_t* tail;
    pthread_mutex_t mutex;
    size_t size;
};

command_queue_t* queue_create(void)
{
    command_queue_t* q = (command_queue_t*)calloc(1, sizeof(command_queue_t));

    if (q == NULL)
    {
        return NULL;
    }

    queue_node_t* dummy_node = (queue_node_t*)calloc(1, sizeof(queue_node_t));

    if (dummy_node == NULL)
    {
        free(q);
        return NULL;
    }

    dummy_node->data = NULL;
    dummy_node->next = NULL;

    q->fake_head = dummy_node;
    q->tail = dummy_node;
    q->size = 1;

    const int mutex_err = pthread_mutex_init(&q->mutex, NULL);

    if (mutex_err != 0)
    {
        free(dummy_node);
        free(q);
        return NULL;
    }
    return q;
}

void queue_destroy(command_queue_t* q, void (*data_destroy_func)(void* data))
{
    if (q == NULL)
    {
        return;
    }

    pthread_mutex_lock(&q->mutex);
    queue_node_t* current = q->fake_head;

    while (current != NULL)
    {
        queue_node_t* next = current->next;

        if (data_destroy_func != NULL && current->data != NULL)
        {
            data_destroy_func(current->data);
        }
        free(current);
        current = next;
    }
    pthread_mutex_destroy(&q->mutex);
    free(q);
}

void queue_push(command_queue_t* q, void* data)
{
    if (q == NULL)
    {
        return;
    }

    queue_node_t* new_node = (queue_node_t*)calloc(1, sizeof(queue_node_t));

    if (new_node == NULL)
    {
        return;
    }

    new_node->data = data;
    new_node->next = NULL;

    pthread_mutex_lock(&q->mutex);

    q->tail->next = new_node;
    q->tail = new_node;
    ++q->size;
    pthread_mutex_unlock(&q->mutex);
}

void* queue_pop(command_queue_t* q)
{
    if (q == NULL)
    {
        return NULL;
    }

    pthread_mutex_lock(&q->mutex);
    queue_node_t* first_node = q->fake_head->next;

    if (first_node == NULL)
    {
        pthread_mutex_unlock(&q->mutex);
        return NULL;
    }

    void* data = first_node->data;
    q->fake_head->next = first_node->next;
    --q->size;

    if (q->tail == first_node)
    {
        q->tail = q->fake_head;
    }

    pthread_mutex_unlock(&q->mutex);
    free(first_node);
    return data;
}

int queue_is_empty(command_queue_t* q)
{
    if (q == NULL)
    {
        return 1;
    }
    pthread_mutex_lock(&q->mutex);
    const int empty = q->size == 1;
    pthread_mutex_unlock(&q->mutex);
    return empty;
}