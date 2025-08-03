from typing import Type, TYPE_CHECKING
from pydantic import BaseModel
from src.app.events import EventType
from src.app.handlers import Handler
from src.core.dto import SystemNoticeBroadcast

if TYPE_CHECKING:
    from src.app.chat_manager import ChatManager

class SystemNoticeBroadcastHandler(Handler):
    def handle(self, manager: "ChatManager", dto: SystemNoticeBroadcast) -> None:
        manager.event_manager.fire(EventType.SYSTEM_NOTICE, data=dto, source_component=self.__class__.__name__, message=None)

    def get_type(self) -> Type[BaseModel]:
        return SystemNoticeBroadcast


system_notice_broadcast_handler = SystemNoticeBroadcastHandler()
