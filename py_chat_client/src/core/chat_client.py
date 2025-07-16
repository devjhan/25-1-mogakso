from src.core import client_lib, ON_ERROR_CALLBACK, ON_MESSAGE_CALLBACK, MessageType
import ctypes

class ChatClient:
    def __init__(self, ip:str, port:int):
        self._ctx = client_lib.client_connect(ip.encode('utf-8'), port)
        if not self._ctx:
            raise ConnectionError(f"서버 연결에 실패했습니다: {ip}:{port}")

        self._on_message_callback = None
        self._on_error_callback = None

    def register_on_message_callback(self, callback):
        self._on_message_callback = ON_MESSAGE_CALLBACK(callback)
        client_lib.client_register_complete_message_callback(self._ctx, self._on_message_callback, None)

    def register_on_error_callback(self, callback):
        self._on_error_callback = ON_ERROR_CALLBACK(callback)
        client_lib.client_register_error_callback(self._ctx, self._on_error_callback, None)

    def send_payload(self, msg_type: MessageType, payload: bytes):
        if not self._ctx:
            raise ConnectionError("클라이언트가 이미 종료되었습니다.")

        payload_buffer = (ctypes.c_uint8 * len(payload))(*payload)
        client_lib.client_send_payload(self._ctx, msg_type.value, payload_buffer, len(payload))

    def start_chat(self):
        """
        채팅 루프를 시작합니다.
        이 함수는 블로킹(blocking)되므로 별도의 스레드에서 실행하는 것이 좋습니다.
        """
        if not self._ctx:
            raise ConnectionError("클라이언트가 이미 종료되었습니다.")
        client_lib.client_start_chat(self._ctx)

    def shutdown(self):
        """
        채팅 루프를 안전하게 종료하도록 신호를 보냅니다.
        'start_chat'이 실행 중인 스레드에서 이 함수를 호출하면 안 됩니다.
        """
        if self._ctx:
            client_lib.client_shutdown(self._ctx)

    def disconnect(self):
        if self._ctx:
            client_lib.client_disconnect(self._ctx)
            self._ctx = None

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.disconnect()






