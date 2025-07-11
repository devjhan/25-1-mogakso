import ctypes
import os
import sys
import threading
from typing import Callable, Optional

try:
    _lib_path = os.path.join(os.path.dirname(__file__), 'libchat.dylib')
    lib = ctypes.CDLL(_lib_path)
except OSError as os_error:
    raise ImportError(f"네이티브 라이브러리 '{_lib_path}' 로드 실패: {os_error}")

# --- C 타입과 ctypes 타입 매핑 정의 ---
ClientContextPtr = ctypes.c_void_p
MessageType = ctypes.c_int
PayloadPtr = ctypes.POINTER(ctypes.c_uint8)

# --- 콜백 함수 타입(Signature) 정의 ---
CLIENT_ON_MESSAGE_CB = ctypes.CFUNCTYPE(None, ctypes.c_void_p, MessageType, PayloadPtr, ctypes.c_size_t)
CLIENT_ON_ERROR_CB = ctypes.CFUNCTYPE(None, ctypes.c_void_p, ctypes.c_int, ctypes.c_char_p)

# --- C 라이브러리 함수 시그니처 정의 ---
# client_connect
lib.client_connect.restype = ClientContextPtr
lib.client_connect.argtypes = [ctypes.c_char_p, ctypes.c_int]

# client_disconnect
lib.client_disconnect.restype = None
lib.client_disconnect.argtypes = [ClientContextPtr]

# client_register_complete_message_callback
lib.client_register_complete_message_callback.restype = None
lib.client_register_complete_message_callback.argtypes = [ClientContextPtr, CLIENT_ON_MESSAGE_CB, ctypes.c_void_p]

# client_register_error_callback
lib.client_register_error_callback.restype = None
lib.client_register_error_callback.argtypes = [ClientContextPtr, CLIENT_ON_ERROR_CB, ctypes.c_void_p]

# client_send_payload
lib.client_send_payload.restype = None
lib.client_send_payload.argtypes = [ClientContextPtr, MessageType, PayloadPtr, ctypes.c_size_t]

# client_send_file
lib.client_send_file.restype = ctypes.c_int
lib.client_send_file.argtypes = [ClientContextPtr, ctypes.c_char_p]

# client_start_chat
lib.client_start_chat.restype = None
lib.client_start_chat.argtypes = [ClientContextPtr]

class ChatClient:
    """
    C 라이브러리를 래핑하여 사용하기 쉬운 객체 지향 인터페이스를 제공하는 채팅 클라이언트 클래스.
    """

    #C 라이브러리가 직접 호출할 static method
    @staticmethod
    def _c_message_handler(user_data_ptr, msg_type, payload_ptr, length):
        """C 라이브러리가 호출하는 진입점. user_data를 이용해 객체 메서드를 호출한다."""
        if not user_data_ptr:
            return

        try:
            client_instance = ctypes.cast(user_data_ptr, ctypes.py_object).value
            client_instance.handle_message(msg_type, payload_ptr, length)
        except Exception as e:
            print(f"[래퍼 내부 오류] 메시지 처리 중 예외 발생: {e}", file=sys.stderr)

    @staticmethod
    def _c_error_handler(user_data_ptr, error_code, message_bytes):
        """C 라이브러리가 호출하는 에러 처리 진입점."""
        if not user_data_ptr:
            return

        try:
            client_instance = ctypes.cast(user_data_ptr, ctypes.py_object).value
            client_instance.handle_error(error_code, message_bytes)
        except Exception as e:
            print(f"[래퍼 내부 오류] 오류 처리 중 예외 발생: {e}", file=sys.stderr)

    def __init__(self, username: str):
        """ChatClient 인스턴스를 초기화합니다."""
        self.username: str = username
        self.is_connected: bool = False

        self._ctx: Optional[ClientContextPtr] = None
        self._chat_thread: Optional[threading.Thread] = None

        self._c_on_message_cb = CLIENT_ON_MESSAGE_CB(self._c_message_handler)
        self._c_on_error_cb = CLIENT_ON_ERROR_CB(self._c_error_handler)

        # 2. GC 방지를 위한 참조 유지 (매우 중요!)
        #    ctypes.py_object로 만든 객체는 Python이 참조를 잃으면 GC 대상이 될 수 있습니다.
        #    인스턴스가 살아있는 동안에는 반드시 참조를 유지해야 합니다.
        self._user_data_ref = None

        # 사용자가 등록할 Python 콜백 핸들러
        self.on_message: Optional[Callable[['ChatClient', int, bytes], None]] = None
        self.on_error: Optional[Callable[['ChatClient', int, str], None]] = None

    def connect(self, ip: str, port: int) -> bool:
        """서버에 연결하고, 성공 시 콜백을 자동으로 등록합니다."""
        if self.is_connected:
            print("경고: 이미 연결되어 있습니다.", file=sys.stderr)
            return False

        ip_bytes = ip.encode('utf-8')
        self._ctx = lib.client_connect(ip_bytes, port)

        if not self._ctx:
            print(f"서버 연결 실패: {ip}:{port}", file=sys.stderr)
            return False

        self.is_connected = True

        # 3. C 라이브러리의 register 함수 호출
        #    - user_data로 'self' 인스턴스를 전달합니다.
        #    - self 인스턴스를 ctypes.py_object로 래핑하여 C에 전달할 포인터를 만듭니다.
        self._user_data_ref = ctypes.py_object(self)
        lib.client_register_complete_message_callback(self._ctx, self._c_on_message_cb, self._user_data_ref)
        lib.client_register_error_callback(self._ctx, self._c_on_error_cb, self._user_data_ref)

        print(f"서버에 성공적으로 연결되었습니다: {ip}:{port}")
        return True

    def disconnect(self):
        """서버와의 연결을 종료하고 자원을 정리합니다."""
        if not self.is_connected or not self._ctx:
            return

        lib.client_disconnect(self._ctx)
        self.is_connected = False
        self._ctx = None
        self._user_data_ref = None

        print("연결이 종료되었습니다.")

        if self._chat_thread and self._chat_thread.is_alive():
            self._chat_thread.join(timeout=2)

    def start_in_background(self):
        """채팅 수신 루프를 백그라운드 스레드에서 시작합니다."""
        if not self.is_connected:
            raise RuntimeError("연결되지 않은 상태에서는 채팅 루프를 시작할 수 없습니다.")
        if self._chat_thread and self._chat_thread.is_alive():
            print("경고: 채팅 루프가 이미 실행 중입니다.", file=sys.stderr)
            return

        self._chat_thread = threading.Thread(target=lib.client_start_chat, args=(self._ctx,))
        self._chat_thread.daemon = True
        self._chat_thread.start()
        print("백그라운드에서 메시지 수신을 시작합니다.")

    def send_message(self, text: str, msg_type: int = 1):
        """텍스트 메시지를 서버로 전송합니다."""
        if not self.is_connected or not self._ctx:
            raise RuntimeError("연결되지 않은 상태에서는 메시지를 보낼 수 없습니다.")

        payload = text.encode('utf-8')
        payload_len = len(payload)

        payload_buffer = (ctypes.c_uint8 * payload_len).from_buffer_copy(payload)
        lib.client_send_payload(self._ctx, msg_type, payload_buffer, payload_len)

    def send_file(self, filepath: str) -> bool:
        """파일을 서버로 전송합니다."""
        if not self.is_connected or not self._ctx:
            raise RuntimeError("연결되지 않은 상태에서는 파일을 보낼 수 없습니다.")

        if not os.path.exists(filepath):
            print(f"오류: 파일이 존재하지 않습니다 - {filepath}", file=sys.stderr)
            return False

        filepath_bytes = filepath.encode('utf-8')
        result = lib.client_send_file(self._ctx, filepath_bytes)
        return result == 0

    def handle_message(self, msg_type: int, payload_ptr: PayloadPtr, length: int):
        """내부 메시지 처리 로직. 사용자가 등록한 핸들러를 호출합니다."""
        if self.on_message:
            payload = ctypes.string_at(payload_ptr, length)
            self.on_message(self, msg_type, payload)

    def handle_error(self, error_code: int, message_bytes: bytes):
        """내부 오류 처리 로직. 사용자가 등록한 핸들러를 호출합니다."""
        if self.on_error:
            message = message_bytes.decode('utf-8', errors='ignore')
            self.on_error(self, error_code, message)
