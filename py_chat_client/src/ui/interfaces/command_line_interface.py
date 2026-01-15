import os
import threading
from typing import TYPE_CHECKING
from src.configs import config
from src.app.events import *
from src.app.chat_manager import chat_manager
from src.core.dto import *
from src.ui.command_handlers import quit_command_handler, nickname_command_handler
from src.ui.command_handlers.command_handler import CommandHandler
from src.ui.command_type import CommandType


if TYPE_CHECKING:
    from src.configs.configuration import Configuration
    from src.app.chat_manager import ChatManager
    from src.app.events.event_manager import EventManager

class CommandLineInterface:
    def __init__(self, _chat_manager: "ChatManager", _event_manager: "EventManager", _config: "Configuration"):
        self.chat_manager: ChatManager = _chat_manager
        self.event_manager: EventManager = _event_manager
        self.config: Configuration = _config

        self._commands: dict[CommandType, CommandHandler] = {
            CommandType.QUIT: quit_command_handler,
            CommandType.EXIT: quit_command_handler,
            CommandType.NICKNAME: nickname_command_handler,
        }

        self._is_logged_in_ui: bool = False
        self._login_event = threading.Event()

        self._is_connected_in_ui: bool = False
        self._connection_event = threading.Event()

        self._subscribe_events()

    def run(self):
        self.chat_manager.connect()
        if not self._connection_event.wait(timeout=5):
            print("서버 연결 응답이 없습니다.")
            return
        if not self._is_connected_in_ui:
            print("연결 실패")
            return

        self.chat_manager.start_daemon()

        max_login_attempts, login_attempt = 3, 0
        # 환경 변수에서 닉네임 가져오기 (자동화 테스트용)
        auto_nickname = os.getenv("CHAT_CLIENT_NICKNAME")
        
        while not self._is_logged_in_ui and login_attempt < max_login_attempts:
            if auto_nickname and login_attempt == 0:
                # 자동 로그인 모드
                nickname = auto_nickname
                print(f"자동 로그인 시도: {nickname}")
            else:
                # 수동 입력 모드
                nickname = input("사용할 닉네임을 입력하세요.(3자 이상, 16자 미만) : ")
            
            self._login_event.clear()
            self.chat_manager.try_login(nickname)
            login_attempt += 1

            if not self._login_event.wait(timeout=10):
                print("서버에서 로그인 응답이 없습니다.")
            if not self._is_logged_in_ui:
                if auto_nickname:
                    # 자동 모드에서 실패 시 바로 종료
                    print(f"자동 로그인에 실패했습니다. (닉네임: {nickname})")
                    break
                else:
                    print(f"로그인에 실패했습니다. 다시 로그인하십시오. ({login_attempt}/{max_login_attempts})")

        if not self._is_logged_in_ui:
            print("로그인 실패")
            return

        while True:
            try:
                user_input = input(f"{self.chat_manager.nickname or "..."}: ")

                if not user_input.startswith("/"):
                    self.chat_manager.send_message(user_input)
                else:
                    self._parse_command(user_input)
            except (KeyboardInterrupt, EOFError):
                print("클라이언트 프로그램을 종료합니다.")
                break
            except Exception as e:
                print(f"알 수 없는 예외 발생: {e}")

    def _parse_command(self, command_str: str):
        parts = command_str.strip().split('/')
        command, args = parts[0], parts[1:]

        command_type = CommandType.from_string(command)
        handler = self._commands.get(command_type, None)

        if handler:
            handler.handle(self.chat_manager, args)
        else:
            print(f"알 수 없는 명령어입니다: {command_str}")
        pass

    def _subscribe_events(self):
        self.event_manager.subscribe(EventType.LOGIN_SUCCESS, self._on_login_success)
        self.event_manager.subscribe(EventType.LOGIN_FAILURE, self._on_login_failure)
        self.event_manager.subscribe(EventType.CONNECTION_SUCCESS, self._on_connection_success)
        self.event_manager.subscribe(EventType.CONNECTION_FAILURE, self._on_connection_failure)
        self.event_manager.subscribe(EventType.NEW_CHAT_MESSAGE, self._on_new_message)
        self.event_manager.subscribe(EventType.FILE_UPLOADED, self._on_file_upload_success)
        self.event_manager.subscribe(EventType.USER_JOINED, self._on_user_joined)
        self.event_manager.subscribe(EventType.USER_LEFT, self._on_user_left)
        self.event_manager.subscribe(EventType.SYSTEM_NOTICE, self._on_system_message)
        if self.config.DEBUG_MODE:
            self.event_manager.subscribe(EventType.INTERNAL_ERROR, self._on_internal_error)
        pass

    def _on_login_success(self, event_data: EventData):
        response: UserLoginResponse = event_data.data
        print(f"로그인 성공! 채팅방에 입장했습니다. (닉네임: {response.nickname})")
        self._is_logged_in_ui = True
        self._login_event.set()

    def _on_login_failure(self, event_data: EventData):
        response: UserLoginResponse = event_data.data
        print(f"\n[오류] 로그인 실패: {event_data.message} 상세: {response.message}")
        self._is_logged_in_ui = False
        self._login_event.set()

    def _on_connection_success(self, event_data: EventData):
        print(f"서버와의 연결 성공 : {event_data.message}")
        self._is_connected_in_ui = True
        self._connection_event.set()

    def _on_connection_failure(self, event_data: EventData):
        response: ErrorResponse = event_data.data
        print(f"\n[오류] 서버와의 연결 실패: {event_data.message} 상세: {response.message}")
        self._is_connected_in_ui = False
        self._connection_event.set()

    def _on_new_message(self, event_data: EventData):
        response: ChatTextBroadcast = event_data.data
        print(f"{response.timestamp.strftime("%dT%H:%M:%S")} {response.author}: {response.content}")

    def _on_file_upload_success(self, event_data: EventData):
        print(f"서버에 {event_data.message}가(이) 업로드되었습니다.")

    def _on_user_joined(self, event_data: EventData):
        response: UserJoinBroadcast = event_data.data
        print(f"새로운 유저 {response.nickname}가(이) 채팅에 참여했습니다.")

    def _on_user_left(self, event_data: EventData):
        response: UserLeaveBroadcast = event_data.data
        print(f"유저 {response.nickname}가(이) 채팅방에서 나갔습니다.")

    def _on_system_message(self, event_data: EventData):
        response: SystemNoticeBroadcast = event_data.data
        print(response.notice)

    def _on_internal_error(self, event_data: EventData):
        response: ErrorResponse = event_data.data
        print(f"{response.timestamp.strftime("%Y-%m-%dT%H:%M:%S")} | {event_data.message} 상세: {response.message}")


command_line_interface = CommandLineInterface(_chat_manager=chat_manager, _event_manager=event_manager, _config=config)
