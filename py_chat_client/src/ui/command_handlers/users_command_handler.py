from typing import Type, List, TYPE_CHECKING
from src.ui.command_handlers import CommandHandler
from src.ui.command_type import CommandType

if TYPE_CHECKING:
    from src.app.chat_manager import ChatManager

class UsersCommandHandler(CommandHandler):
    def handle(self, manager: "ChatManager", args: List[str]) -> None:
        pass

    def get_type(self) -> Type[CommandType]:
        return CommandType.USER_COMMAND

users_command_handler = UsersCommandHandler()