from .wrapper import *
from .enums import *
from .dto import *
import hashlib
import ctypes
import os

class ChatClient:
    def __init__(self, ip:str, port:int, chunk_size: int = 4096):
        self._ctx = client_lib.client_connect(ip.encode('utf-8'), port)

        if not self._ctx:
            raise ConnectionError(f"서버 연결에 실패했습니다: {ip}:{port}")

        self._on_message_callback = None
        self._on_error_callback = None
        self.chunk_size = chunk_size

    def register_on_message_callback(self, callback: ON_MESSAGE_CALLBACK, user_data: ctypes.c_void_p = None):
        self._on_message_callback = ON_MESSAGE_CALLBACK(callback)
        client_lib.client_register_complete_message_callback(self._ctx, self._on_message_callback, user_data)

    def register_on_error_callback(self, callback: ON_ERROR_CALLBACK, user_data: ctypes.c_void_p = None):
        self._on_error_callback = ON_ERROR_CALLBACK(callback)
        client_lib.client_register_error_callback(self._ctx, self._on_error_callback, user_data)

    def send_login_request(self, request: UserLoginRequest):
        payload_bytes = request.model_dump_json().encode('utf-8')
        self._send_payload(MessageType.MSG_TYPE_USER_LOGIN_REQUEST, payload_bytes)

    def send_chat_text(self, request: ChatTextRequest):
        payload_bytes = request.model_dump_json().encode('utf-8')
        self._send_payload(MessageType.MSG_TYPE_CHAT_TEXT, payload_bytes)

    def send_file(self, file_path: str):
        if not os.path.exists(file_path):
            raise FileNotFoundError(f"파일을 찾을 수 없습니다: {file_path}")

        if not os.path.isfile(file_path):
            raise IsADirectoryError(f"경로는 파일이 아닌 디렉토리입니다: {file_path}")

        filename = os.path.basename(file_path)
        filesize = os.path.getsize(file_path)

        try:
            file_start_request = FileStartRequest(filename=filename, filesize=filesize)
            self._send_file_start(file_start_request)
            print(f"파일 전송 시작: {filename} ({filesize} bytes)")

            _hash = hashlib.sha256()
            with open(file_path, 'rb') as f:
                while True:
                    chunk = f.read(self.chunk_size)
                    if not chunk:
                        break

                    _hash.update(chunk)
                    self._send_payload(MessageType.MSG_TYPE_FILE_CHUNK, chunk)

            checksum = _hash.hexdigest()
            file_end_request = FileEndRequest(filename=filename, checksum=checksum)
            self._send_file_end(file_end_request)
            print(f"파일 전송 완료. 체크섬: {checksum}")
        except PermissionError as e:
            raise RuntimeError(f"파일의 권한에 관련된 오류가 발생했습니다: {e}")
        except ConnectionError as e:
            raise ConnectionError(f"전송 중 연결 오류가 발생했습니다: {e}")
        except IOError as e:
            raise RuntimeError(f"파일을 읽는 중 오류가 발생했습니다: {e}")
        except Exception as e:
            raise RuntimeError(f"알 수 없는 오류가 발생했습니다: {e}")

    def _send_file_start(self, request: FileStartRequest):
        payload_bytes = request.model_dump_json().encode('utf-8')
        self._send_payload(MessageType.MSG_TYPE_FILE_INFO, payload_bytes)

    def _send_file_end(self, request: FileEndRequest):
        payload_bytes = request.model_dump_json().encode('utf-8')
        self._send_payload(MessageType.MSG_TYPE_FILE_END, payload_bytes)

    def _send_payload(self, msg_type: MessageType, payload: bytes):
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






