from typing import Type, TYPE_CHECKING
from pydantic import BaseModel
from src.app.events import EventType
from src.app.handlers import Handler
from src.core.dto import ChatTextBroadcast

if TYPE_CHECKING:
    from src.app.chat_manager import ChatManager

class ChatTextBroadcastHandler(Handler):
    def handle(self, manager: "ChatManager", dto: ChatTextBroadcast) -> None:
        if dto.author == manager.nickname:
            return

        manager.event_manager.fire(EventType.NEW_CHAT_MESSAGE, data=dto, source_component=self.__class__.__name__, message="새 메시지가 도착했습니다.")

    def get_type(self) -> Type[BaseModel]:
        return ChatTextBroadcast


chat_text_broadcast_handler = ChatTextBroadcastHandler()
