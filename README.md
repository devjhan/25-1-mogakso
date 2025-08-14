# 학습 노트
---
# DAY0 : 학습 계획 수립
## 프로젝트 주제 : 소켓 통신 학습
## 프로젝트 예상 성과 : 채팅 라이브러리, 클라이언트, 서버의 구현
## 구체적인 목표 : 
1. C언어 등을 이용한 저수준 소켓 통신 라이브러리의 구현
2. 위 라이브러리와 Java SpringBoot 프레임워크를 이용한 채팅 서버의 구현
3. 위 라이브러리와 Python을 이용한 채팅 클라이언트 프로그램의 구현
---
# DAY1 : C 언어를 이용한 저수준 소켓 통신 라이브러리의 구현
## 계획 : 
- 기능 단위로 라이브러리 분할. 
  - client_lib : 클라이언트 어플리케이션에 노출되는 함수, 구조체 등을 정의
  - server_lib : 서버 어플리케이션에 노출되는 함수, 구조체 등을 정의
  - common : 서버, 클라이언트가 공통적으로 사용하는 프로토콜, 파싱 로직 등을 정의
  - socket_lib : 위 ```/client_lib```, ```/server_lib```, ```/common```에서 공통으로 호출하는 커널 함수를 래핑하여 플랫폼 종속성을 통제
- 비동기 채팅 프로그램의 구현을 위하여 ```/common``` 디렉토리에 ```command_queue```를 구현하여 순차적으로 명령어를 처리함
- 헤더와 구현부 분리
  - 각 라이브러리의 ```/include``` 디렉토리에는 외부 모듈, 상위 레이어에 공개할 함수의 시그니처, 구조체의 정의 등을 포함
  - 각 라이브러리의 ```/src``` 디렉토리에는 실제 소스 코드를 포함
- CMake를 이용하여 빌드
---
##DAY2 : SpringBoot를 이용한 자바 백엔드 서버 작성
## 계획 : 
- 일반적인 SpringBoot 라이브러리 관습에 따라 패키지 분할.
  - wrapper_library : C 라이브러리를 JAVA 프로젝트에서 사용할 수 있도록 인터페이스를 구현하고, 어플리케이션 레이어에서 직관적으로 활용할 수 있는 API를 다른 패키지에 제공.
  - config : applications.properties 파일과 연동하여 서버 IP, Port와 같은 의존성 주입, wrapper_library의 생명주기를 통제.
  - dto : 어플리케이션 레이어 단위에서, 클라이언트 프로그램과의 통신에 필요한 프로토콜을 정의하고 구현.
  - domain : 서버 어플리케이션 내부적으로 필요한 클래스 정의.
  - runner : 서버 어플리케이션의 전체적인 생명주기를 통제. 
  - service : 비즈니스 로직을 정의.
## 상세 :
- service 패키지는 내부적으로 역할에 따라 ChatService, UserService, FileTransferService로 분할.
- ChatService는 내부적으로 수신한 메시지의 MessageType(C 라이브러리 정의)에 따라 각각의 Handler를 호출하여 클라이언트에 dto를 송신.
- Handler는 클라이언트와 직접적인 통신을 하지 않고, HandlerResult라는 클래스를 반환하여, ChatService에 클라이언트와의 송수신 기능을 집중시켜 계층 간의 역할 분할을 명확히 함.
- 직접 작성한 라이브러리를 사용하기 때문에 플랫폼에 따라 C 라이브러리 파일이 다른데, 이를 JNA 라이브러리를 사용하여 해결함. resources 라이브러리에 플랫폼 코드별로 각각의 라이브러리 파일을 포함하는 구조로 작성. 사용한 노트북이 M1 맥북이기 때문에 ```resources/darwin-aarch64``` 경로에 작성. 추후 다른 플랫폼에서 빌드할 경우, ```resources```  경로에 패키지를 추가하여 확장할 수 있음.
---
## DAY3 : 파이썬을 이용한 클라이언트 프로그램 작성
## 계획 : 
- app : C 라이브러리 래퍼 클래스를 소유하고, 편리하게 사용할 수 있는 인터페이스를 ui 계층에 제공.
- configs : .env 파일로부터 의존성을 주입받고, 다른 패키지에 제공.
- core : C 라이브러리를 파이썬 프로젝트에서 사용할 수 있도록 인터페이스를 구현하고, 어플리케이션 레이어에서 직관적으로 활용할 수 있는 API를 다른 패키지에 제공.
- ui : 사용자가 간편하게 사용할 수 있는 인터페이스 제공.
- main.py : 어플리케이션을 구동.
## 상세 :
- 서버와 유사하게, 수신한 메시지의 MessageType(C 라이브러리 정의)에 따라 각각의 Handler를 호출하여 이벤트를 격발.
- 모든 이벤트는 EventManager에 의해 통제되고, EventType과 EventData를 통해 이벤트의 종류와 작업을 정의함.
- 각각의 계층에서는 EventManager에 구독할 이벤트의 종류와 콜백을 저장하여 실행 흐름을 제어함.
---
## DAY4 : C 라이브러리 작성 1
## socket_lib
- 계층 분리 원칙을 적용하여 대응하는 리눅스 함수를 직접 호출한뒤, 별도의 처리 없이 즉시 반환하는 형태로 작성.
- 에러 처리는 상위 계층(```client_lib, server_lib```)으로 완전히 위임.
- OS 종속적인 함수를 상위 계층에서 직접적으로 호출할 경우, 라이브러리 전체가 해당 OS에 종속되기 때문에 별도의 계층으로 분리.
```c
// socket_utils.c 일부 첨부함
int create_tcp_socket(void)
{
    return socket(AF_INET, SOCK_STREAM, 0);
}
```
## common
### protocol
- 메모리에서 사용하는 포인터(참조값)는 해당 컴퓨터 내부적으로만 유효한 값이기 때문에 네트워크 통신 중에는 사용할 수 없음.  
- 네트워크 통신 중에는 데이터를 정해진 규약에 따라 추출하고(직렬화), 다시 복원(역직렬화)할 필요가 있음.
- 위 역할을 수행하는 프로그램을 파서(```parser```)라고 부르며, 프로그램 내부적으로 정의하는 규약을 프로토콜(```protocol```)이라고 칭함.
- 파서는 내부적으로 ```PARSER_STATE_WANT_HEADER, PARSER_STATE_WANT_PAYLOAD```의 두가지 상태를 가지며, 고정 길이의 헤더를 전부 받았을 경우, ```PARSER_STATE_WANT_HEADER -> PARSER_STATE_WANT_PAYLOAD```로 상태를 바꿈.
- payload의 파싱이 완료된 경우, 파라미터로 주입받은 콜백을 호출하여 상위 계층에 알림.
```c
// protocol.c 소스코드 일부 첨부함
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
```
### command_queue
- 서버가 논리적으로 단일 쓰레드만으로 여러 클라이언트의 요청을 처리하기 위해서는 클라이언트의 요청을 저장할 필요가 있음.
- 큐 자료구조를 이용하여 단순한 형태로 구현함.
- 큐의 구현상 편의를 위하여 더미 노드 한개를 가지고 있는 형태로 구현함. 이런 구조로 구현할 경우, 큐에 데이터를 삽입할 때, 큐가 비어있든 아니든 동일한 형태로 구현할 수 있음.
- 서버 라이브러리 자체는 싱글 쓰레드 기반이지만, 추후를 대비하여 쓰레드 안전하게 구성함.
```c
// command_queue.c 소스코드 일부 첨부함.
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
```
### command
- ```command_queue```에 들어가는 개별 요청을 공통된 규칙으로 처리하기 위하여 내부적으로 사용하는 규약.
## 디테일 : 
- 소스 파일(```.c```)에 헤더 파일(```.h```)을 포함하는 편이 유리하다. 이렇게 작성할 경우, ```CMakeLists.txt```에 헤더 파일을 수동으로 추가하지 않아도 IDE의 syntax hightlighting 기능을 사용할 수 있음.

