from typing import Type, List, TYPE_CHECKING
from src.app.events import EventType
from src.core.dto import SystemNoticeBroadcast
from src.ui.command_handlers import CommandHandler
from src.ui.command_type import CommandType

if TYPE_CHECKING:
    from src.app.chat_manager import ChatManager

class NicknameCommandHandler(CommandHandler):
    def handle(self, manager: "ChatManager", args: List[str]) -> None:
        dto=  SystemNoticeBroadcast(notice=f"[SYSTEM] {manager.nickname}입니다.")
        manager.event_manager.fire(EventType.SYSTEM_NOTICE, data=dto, source_component=self.__class__.__name__, message=None)

    def get_type(self) -> Type[CommandType]:
        return CommandType.NICKNAME

nickname_command_handler = NicknameCommandHandler()