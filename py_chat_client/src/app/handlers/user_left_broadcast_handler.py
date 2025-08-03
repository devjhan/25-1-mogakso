from typing import Type, TYPE_CHECKING
from pydantic import BaseModel
from src.app.events import EventType
from src.app.handlers import Handler
from src.core.dto import UserLeaveBroadcast

if TYPE_CHECKING:
    from src.app.chat_manager import ChatManager

class UserLeftBroadcastHandler(Handler):
    def handle(self, manager: "ChatManager", dto: UserLeaveBroadcast) -> None:
        if manager.nickname == dto.nickname:
            return
        manager.event_manager.fire(EventType.USER_LEFT, data=dto, source_component=self.__class__.__name__, message=None)

    def get_type(self) -> Type[BaseModel]:
        return UserLeaveBroadcast


user_left_broadcast_handler = UserLeftBroadcastHandler()
