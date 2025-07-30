from typing import Type, List
from src.app.chat_manager import ChatManager
from src.ui.command_handlers.command_handler import CommandHandler
from src.ui.command_type import CommandType

class QuitCommandHandler(CommandHandler):
    def handle(self, manager: ChatManager, args: List[str]) -> None:
        manager.disconnect()

    def get_type(self) -> Type[CommandType]:
        return CommandType.QUIT

quit_command_handler = QuitCommandHandler()