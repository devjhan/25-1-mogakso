from abc import ABC, abstractmethod
from typing import TYPE_CHECKING, Type, List
from src.ui.command_type import CommandType

if TYPE_CHECKING:
    from src.app.chat_manager import ChatManager

class CommandHandler(ABC):
    @abstractmethod
    def handle(self, manager: "ChatManager", args: List[str]) -> None:
        pass

    @abstractmethod
    def get_type(self) -> Type[CommandType]:
        pass
