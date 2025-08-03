import os
from datetime import datetime
from typing import TYPE_CHECKING, Callable, Optional, Any
from threading import Thread
from src.app.handlers import *
from src.configs import *
from src.core.dto import *
from src.core.enums import *
from src.core.wrapper import *
from src.core.chat_client import ChatClient
from src.app.events import *
import ctypes

if TYPE_CHECKING:
    from src.configs.configuration import Configuration
    from src.app.events.event_manager import EventManager

class ChatManager:
    def __init__(self, _config: "Configuration", _event_manager: "EventManager"):
        self.config: Configuration = _config
        self.event_manager: EventManager = _event_manager
        self._handlers: dict[MessageType, Handler] = {
            MessageType.MSG_TYPE_USER_LOGIN_RESPONSE: user_login_response_handler,
            MessageType.MSG_TYPE_USER_JOIN_NOTICE: user_join_broadcast_handler,
            MessageType.MSG_TYPE_USER_LEAVE_NOTICE: user_left_broadcast_handler,
            MessageType.MSG_TYPE_SERVER_NOTICE: system_notice_broadcast_handler,
            MessageType.MSG_TYPE_ERROR_RESPONSE: error_response_handler,
            MessageType.MSG_TYPE_CHAT_TEXT: chat_text_broadcast_handler
        }

        self.nickname: Optional[str] = None
        self.client_id: Optional[int] = None

        self._client: Optional[ChatClient] = None
        self._chat_thread: Optional[Thread] = None

        self._on_message: ON_MESSAGE_CALLBACK = None
        self._on_error: ON_ERROR_CALLBACK = None
        self._user_data: ctypes.c_void_p = ctypes.cast(ctypes.pointer(ctypes.py_object(self)), ctypes.c_void_p)

    def is_logged_in(self) -> bool:
        return self.nickname is not None

    def is_connected(self) -> bool:
        return self._client is not None

    def connect(self):
        if not self.is_connected():
            try:
                self._client = ChatClient(self.config.SERVER_IP, self.config.SERVER_PORT, self.config.CHUNK_SIZE)
                self.register_on_message(self._handle_message_proxy, self._user_data)
                self.register_on_error(self._handle_error_proxy, self._user_data)
                self.event_manager.fire(EventType.CONNECTION_SUCCESS, data=None, source_component=self.__class__.__name__, message="서버에 성공적으로 연결했습니다..")
            except ConnectionError as e:
                self._client = None
                error_response = ErrorResponse(errorCode="CONNECTION_FAILED", message=str(e), timestamp=datetime.now())
                self.event_manager.fire(EventType.CONNECTION_FAILURE, data=error_response, source_component=self.__class__.__name__, message="서버와의 연결에 실패했습니다.")

    def try_login(self, nickname: str):
        if not self.is_connected():
            failed_response = UserLoginResponse(success=False, message="서버에 연결되지 않았습니다.", nickname="", clientId=-1)
            self.event_manager.fire(EventType.LOGIN_FAILURE, data=failed_response, source_component=self.__class__.__name__, message="서버와 먼저 연결하고 로그인을 시도하십시오.")
            return
        if len(nickname) < 3 or len(nickname) > 16:
            failed_response = UserLoginResponse(success=False, message="닉네임은 3자 이상 16자 이하여야 합니다.", nickname="", clientId=-1)
            self.event_manager.fire(EventType.LOGIN_FAILURE, data=failed_response, source_component=self.__class__.__name__, message="닉네임 제약을 위반했습니다.")
            return

        try:
            login_request = UserLoginRequest(nickname=nickname)
            self._client.send_login_request(login_request)
        except ConnectionError as e:
            failed_response = UserLoginResponse(success=False, message=str(e), nickname="", clientId=-1)
            self.event_manager.fire(EventType.LOGIN_FAILURE, data=failed_response, source_component=self.__class__.__name__, message="서버에 연결되지 않았습니다.")

    def start_daemon(self):
        if self.is_connected() and not self._chat_thread:
            self._chat_thread = Thread(target=self._client.start_chat, daemon=True)
            self._chat_thread.start()
            self.event_manager.fire(EventType.CONNECTION_FAILURE, data=None, source_component=self.__class__.__name__, message="리스너 생성에 성공했습니다.")
        elif not self.is_connected():
            error_response = ErrorResponse(errorCode="NOT_CONNECTED", message="클라이언트가 연결되지 않아 리스너를 시작할 수 없습니다.", timestamp=datetime.now())
            self.event_manager.fire(EventType.INTERNAL_ERROR, data=error_response, source_component=self.__class__.__name__, message="서버와 먼저 연결하고 시도하십시오.")

    def disconnect(self):
        if self.is_connected():
            self._client.shutdown()

        if self._chat_thread and self._chat_thread.is_alive():
            self._chat_thread.join(timeout=5.0)

        if self._client:
            self._client.disconnect()

        self._client = None
        self._chat_thread = None
        self.nickname = None
        self.client_id = None
        self.event_manager.fire(EventType.DISCONNECTED, data=None, source_component=self.__class__.__name__, message="연결이 종료되었습니다.")

    def handle_message(self, msg_type_val: int, payload_ptr, payload_len: int) -> None:
        try:
            payload_data: bytes = payload_ptr[:payload_len]
            payload: str = bytes(payload_data).decode('utf-8')
            msg_type = MessageType(msg_type_val)
            handler = self._handlers.get(msg_type)

            if handler:
                dto = handler.get_type().model_validate_json(payload)
                handler.handle(self, dto)
            else:
                self.event_manager.fire(EventType.WARNING, data=None, source_component=self.__class__.__name__, message="경고: 정의되지 않은 메시지 타입을 수신했습니다.")
        except Exception as e:
            error_response = ErrorResponse(errorCode="UNKNOWN", message=str(e), timestamp=datetime.now())
            self.event_manager.fire(EventType.INTERNAL_ERROR, data=error_response, source_component=self.__class__.__name__, message="알 수 없는 오류 발생")

    def handle_error(self, error_code: int, message_ptr: ctypes.c_char_p):
        try:
            error_message: str = message_ptr.value.decode('utf-8', errors='ignore')
            error_response = ErrorResponse(errorCode=f"C_LIBRARY_ERROR({error_code})", message=error_message, timestamp=datetime.now())
            self.event_manager.fire(EventType.INTERNAL_ERROR,data=error_response, source_component=self.__class__.__name__, message="C 라이브러리에서 오류가 발생했습니다.")
        except Exception as e:
            error_response = ErrorResponse(errorCode="UNKNOWN", message=str(e), timestamp=datetime.now())
            self.event_manager.fire(EventType.INTERNAL_ERROR, data=error_response, source_component=self.__class__.__name__, message="알 수 없는 오류 발생")

    def register_on_message(self, callback: Callable, user_data: Any):
        if not self._client:
            return

        self._on_message = ON_MESSAGE_CALLBACK(callback)
        self._client.register_on_message_callback(self._on_message, user_data)

    def register_on_error(self, callback: Callable, user_data: Any):
        if not self._client:
            return

        self._on_error = ON_ERROR_CALLBACK(callback)
        self._client.register_on_error_callback(self._on_error, user_data)

    def send_message(self, message: str):
        if not self.is_logged_in():
            error_response = ErrorResponse(errorCode="AUTH_REQUIRED", message="로그인되지 않았습니다.", timestamp=datetime.now())
            self.event_manager.fire(EventType.INTERNAL_ERROR, data=error_response, source_component=self.__class__.__name__, message="채팅을 보내려면 먼저 로그인이 필요합니다.")
            return

        if not message or not message.strip():
            return

        try:
            request = ChatTextRequest(message=message.strip())
            self._client.send_chat_text(request)
        except ConnectionError as e:
            error_response = ErrorResponse(errorCode="CONNECTION_FAILED", message=str(e), timestamp=datetime.now())
            self.event_manager.fire(EventType.INTERNAL_ERROR, data=error_response, source_component=self.__class__.__name__, message="메시지 전송 중 알 수 없는 오류 발생")
        except Exception as e:
            error_response = ErrorResponse(errorCode="UNKNOWN", message=str(e), timestamp=datetime.now())
            self.event_manager.fire(EventType.INTERNAL_ERROR, data=error_response, source_component=self.__class__.__name__, message="알 수 없는 오류 발생")

    def send_file(self, file_path: str):
        if not self.is_logged_in():
            error_response = ErrorResponse(errorCode="AUTH_REQUIRED", message="로그인되지 않았습니다.", timestamp=datetime.now())
            self.event_manager.fire(EventType.INTERNAL_ERROR, data=error_response, source_component=self.__class__.__name__, message="파일을 보내려면 먼저 로그인이 필요합니다.")
            return

        if not file_path or not file_path.strip():
            return

        try:
            if not os.path.exists(file_path):
                error_response = ErrorResponse(errorCode="NOT_FOUND", message=file_path, timestamp=datetime.now())
                self.event_manager.fire(EventType.INTERNAL_ERROR, data=error_response, source_component=self.__class__.__name__, message="파일을 찾을 수 없습니다")
                return

            if not os.path.isfile(file_path):
                error_response = ErrorResponse(errorCode="NOT_FILE", message=file_path, timestamp=datetime.now())
                self.event_manager.fire(EventType.INTERNAL_ERROR, data=error_response, source_component=self.__class__.__name__, message="경로는 파일이 아닌 디렉토리입니다")
                return

            self._client.send_file(file_path)

            file_name = os.path.basename(file_path)
            self.event_manager.fire(EventType.FILE_UPLOADED, data=None, source_component=self.__class__.__name__, message=file_name)
        except PermissionError as e:
            error_response = ErrorResponse(errorCode="PERMISSION_REQUIRED", message=f"{file_path} : {str(e)}", timestamp=datetime.now())
            self.event_manager.fire(EventType.INTERNAL_ERROR, data=error_response, source_component=self.__class__.__name__, message="파일을 읽을 권한이 없습니다")
        except ConnectionError as e:
            error_response = ErrorResponse(errorCode="CONNECTION_FAILED", message=f"{file_path} : {str(e)}",timestamp=datetime.now())
            self.event_manager.fire(EventType.INTERNAL_ERROR, data=error_response, source_component=self.__class__.__name__, message="파일 전송 중 오류 발생")
        except Exception as e:
            error_response = ErrorResponse(errorCode="UNKNOWN", message=f"{file_path} : {str(e)}",timestamp=datetime.now())
            self.event_manager.fire(EventType.INTERNAL_ERROR, data=error_response, source_component=self.__class__.__name__, message="알 수 없는 오류 발생")

    @staticmethod
    def _handle_message_proxy(user_data, msg_type_val, payload_ptr, payload_len):
        instance = ctypes.cast(user_data, ctypes.POINTER(ctypes.py_object)).contents.value
        instance.handle_message(msg_type_val, payload_ptr, payload_len)

    @staticmethod
    def _handle_error_proxy(user_data, error_code, message_ptr):
        instance = ctypes.cast(user_data, ctypes.POINTER(ctypes.py_object)).contents.value
        instance.handle_error(error_code, message_ptr)


chat_manager = ChatManager(_config=config, _event_manager=event_manager)

