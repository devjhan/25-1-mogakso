from typing import Type, TYPE_CHECKING
from pydantic import BaseModel
from src.app.events import EventType
from src.app.handlers import Handler
from src.core.dto import UserJoinBroadcast

if TYPE_CHECKING:
    from src.app.chat_manager import ChatManager

class UserJoinBroadcastHandler(Handler):
    def handle(self, manager: "ChatManager", dto: UserJoinBroadcast) -> None:
        if manager.nickname == dto.nickname:
            return

        manager.event_manager.fire(EventType.USER_JOINED, data=dto, source_component=self.__class__.__name__, message=None)

    def get_type(self) -> Type[BaseModel]:
        return UserJoinBroadcast


user_join_broadcast_handler = UserJoinBroadcastHandler()
