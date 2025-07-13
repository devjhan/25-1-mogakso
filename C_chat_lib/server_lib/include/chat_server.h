//
// Created by jhan_macbook on 25. 6. 26.
//

#ifndef CHAT_SERVER_H
#define CHAT_SERVER_H

#ifdef __cplusplus
extern "C"
{
	#endif
#include <command_queue.h>

	#include "protocol.h"
	#include <pthread.h>
	#include <poll.h>
	#define MAX_FD_LIMIT 4096

	typedef enum
	{
		SERVER_STATE_STOPPED,
		SERVER_STATE_SHUTTING_DOWN,
		SERVER_STATE_RUNNING
	} server_state_t;

	typedef struct
	{
    	int socket_fd;
    	char ip_addr[16];
		stream_parser_t* client_parser;
	} client_info_t;

	typedef void (*server_on_client_connected_callback)(void* user_data, const client_info_t* client);
	typedef void (*server_on_complete_message_received_callback)(void* user_data, const client_info_t* client, const message_type_t msg_type, const uint8_t* payload, const size_t len);
	typedef void (*server_on_client_disconnected_callback)(void* user_data, const client_info_t* client);
	typedef void (*server_on_error_callback)(void* user_data, const int error_code, const char* message);

	typedef struct
	{
		int listening_socket_fd;
		int port;
		client_info_t* clients;
		client_info_t* client_map[MAX_FD_LIMIT];
		int max_clients;
		int client_count;
		struct pollfd* pollers;
		int shutdown_pipe[2];
		int command_pipe[2];
		command_queue_t* command_queue;
		pthread_t server_thread;
		server_state_t server_state;
		pthread_mutex_t state_mutex;
		char mutex_inited;
		server_on_client_connected_callback on_connect_cb;
		void* connect_user_data;
		server_on_complete_message_received_callback on_complete_message_cb;
		void* completed_message_user_data;
		server_on_client_disconnected_callback on_disconnect_cb;
		void* disconnect_user_data;
		server_on_error_callback on_error_cb;
		void* error_user_data;
	} server_context_t;

	typedef struct
	{
		server_context_t* server_context;
		const client_info_t* client_info;
	} message_context_t;

	/**
	* @brief 클라이언트 연결 시 호출될 콜백 함수를 등록합니다.
	* @param stx 서버 컨텍스트
	* @param callback 호출될 함수 포인터
	* @param user_data 콜백 함수 호출 시 첫 번째 인자로 전달될 사용자 정의 데이터
	*/
	void server_register_connect_callback(server_context_t* stx, const server_on_client_connected_callback callback, void* user_data);

	/**
	* @brief 메시지 수신 시 호출될 콜백 함수를 등록합니다.
	* @param stx 서버 컨텍스트
	* @param callback 호출될 함수 포인터
	* @param user_data 콜백 함수 호출 시 첫 번째 인자로 전달될 사용자 정의 데이터
	*/
	void server_register_complete_message_callback(server_context_t* stx, const server_on_complete_message_received_callback callback, void* user_data);

	/**
	* @brief 클라이언트와 연결 해제 시에 호출될 콜백 함수를 등록합니다.
	* @param stx 서버 컨텍스트
	* @param callback 호출될 함수 포인터
	* @param user_data 콜백 함수 호출 시 첫 번째 인자로 전달될 사용자 정의 데이터
	*/
	void server_register_disconnect_callback(server_context_t* stx, const server_on_client_disconnected_callback callback, void* user_data);

	/**
	* @brief 서버 에러 발생 시 호출될 콜백 함수를 등록합니다.
	* @param stx 서버 컨텍스트
	* @param callback 호출될 함수 포인터
	* @param user_data 콜백 함수 호출 시 첫 번째 인자로 전달될 사용자 정의 데이터
	*/
	void server_register_error_callback(server_context_t* stx, const server_on_error_callback callback, void* user_data);

	/**
	* @brief 채팅 서버 컨텍스트를 생성하고 초기화합니다.
	* @details 내부적으로 소켓 생성, SO_REUSEADDR 설정, bind, listen을 수행합니다.
	* @param port 서버가 리스닝할 포트 번호
	* @param max_clients 동시에 처리할 최대 클라이언트 수
	* @return 성공 시 초기화된 server_context_t 포인터, 실패 시 NULL
	*/
	server_context_t* server_create(const int port, const int max_clients);

	/**
	 * @brief 서버를 안전하게 종료합니다.
	* @param stx 서버 컨텍스트
	*/
	void server_shutdown(server_context_t* stx);

	/**
	* @brief 서버에 할당된 모든 자원을 해제합니다.
	* @param stx 서버 컨텍스트
	*/
	void server_destroy(server_context_t* stx);

	/**
	* @brief 서버의 메인 이벤트 루프를 시작합니다.
	* @details 백그라운드에서 서버의 메인 이벤트 루프를 호출합니다.
	* @param stx 서버 컨텍스트
	* @return 성공 시 0, 치명적인 오류 발생 시 -1
	*/
	int server_start(server_context_t* stx);

	/**
	* @brief 특정 클라이언트에게 메시지를 전송합니다.
	* @param stx 서버 컨텍스트
	* @param client_fd 메시지를 전송할 클라이언트의 소켓 파일 디스크립터
	* @param msg_type 전송할 payload의 타입
	* @param payload 전송할 bytestream
	* @param payload_len 전송할 payload의 길이
	* @return 성공 시 0, 실패 시 -1
	*/
	int server_send_payload_to_client(server_context_t* stx, const int client_fd, const message_type_t msg_type, const uint8_t* payload, const size_t payload_len);

	/**
	* @brief 연결된 모든 클라이언트에게 메시지를 브로드캐스트합니다.
	* @param stx 서버 컨텍스트
	* @param msg_type 보낼 메시지의 타입
	* @param payload 브로드캐스트할 메시지
	* @param payload_len 전송할 payload의 길이
	* @param exclude_fd 이 파일 디스크립터를 가진 클라이언트는 제외 (메시지를 보낸 클라이언트에게 다시 보내지 않기 위함)
	* @return 성공 시 0, 실패 시 -1
	*/
	int server_broadcast_message(server_context_t* stx, const message_type_t msg_type, const uint8_t* payload, const size_t payload_len, const int exclude_fd);

    #ifdef __cplusplus
}
#endif
#endif //CHAT_SERVER_H
