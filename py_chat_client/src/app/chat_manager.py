from threading import Thread
from src.core.chat_client import ChatClient
from typing import Optional, List
from src.app.events import EventHook

class ChatManager:
    def __init__(self, config):
        self.config = config
        self._client : Optional[ChatClient] = None
        self._config : Optional[ChatClient] = None
        self._chat_thread : Optional[Thread] = None

        self.username : Optional[str] = None
        self.is_connected : bool = False
        self.is_logged_in : bool = False
        self.messages : List[str] = []

        self.on_connection_status_changed = EventHook()
        self.on_login_result = EventHook()
        self.on_new_message_received = EventHook()
        self.on_error_occurred = EventHook()

    def connect_and_login(self, username: str):
        if self.is_connected:
            print("already connected.")
            return

        self.username = username

    def send_chat_message(self, message: str):
        pass

    def disconnect(self):
        pass

    def _request_login(self):
        pass

    def _handle_message(self, user_data, msg_type_int, payload_ptr, payload_len):
        pass

    def _handle_error(self, user_data, error_code, message_ptr):
        pass

    def _cleanup_resources(self):
        if self._client:
            self._client.disconnect()

        self._client = None
        self._chat_thread = None
        self.is_connected = False
        self.is_logged_in = False
        self.username = None
        pass


